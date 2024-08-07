/*
 * Copyright 2015-2024 Open Text.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.worker.queue.sqs;

import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.TaskInformation;
import com.hpe.caf.api.worker.WorkerQueueMetricsReporter;
import com.hpe.caf.configs.SQSConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

public final class SQSWorkerQueue implements ManagedWorkerQueue
{
    private SqsClient sqsClient;
    private Thread consumerThread;
    private String inputQueueUrl;

    private final SQSWorkerQueueConfiguration sqsQueueConfiguration;
    private final SQSConfiguration sqsConfiguration;
    private final Map<String, QueueInfo> declaredQueues = new ConcurrentHashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(SQSWorkerQueue.class);
    private static final SQSClientProviderImpl clientProvider = new SQSClientProviderImpl();

    public SQSWorkerQueue(final SQSWorkerQueueConfiguration sqsQueueConfiguration)
    {
        this.sqsQueueConfiguration = Objects.requireNonNull(sqsQueueConfiguration);
        sqsConfiguration = sqsQueueConfiguration.getSQSConfiguration();
    }

    public void start(final TaskCallback callback) throws QueueException
    {
        if (sqsClient != null) {
            throw new IllegalStateException("Already started");
        }
        try {
            sqsClient = clientProvider.getSqsClient(sqsConfiguration);
            inputQueueUrl = createQueue(sqsQueueConfiguration.getInputQueue()).url();
            var consumer = new SQSQueueConsumer(sqsClient, inputQueueUrl, callback, sqsQueueConfiguration);

            consumerThread = new Thread(consumer);
            consumerThread.start();
        } catch (final Exception e) {
            throw new QueueException("Failed to start worker queue", e);
        }
    }

    /**
     * @param queueName
     * @return
     */
    public QueueInfo createQueue(final String queueName)
    {
        if (!declaredQueues.containsKey(queueName)) {
            try {
                final var attributes = getAttributes();
                final var createQueueRequest = CreateQueueRequest.builder()
                        .queueName(queueName)
                        .attributes(attributes)
                        .build();

                final var response = sqsClient.createQueue(createQueueRequest);
                final var url = response.queueUrl();
                final var arn = SQSUtil.getQueueArn(sqsClient, url);
                declaredQueues.put(queueName, new QueueInfo(url, arn));
            } catch (final QueueNameExistsException e) {
                LOG.info("Queue already exists {} {}", queueName, e.getMessage());
                var url = SQSUtil.getQueueUrl(sqsClient, queueName);
                final var arn = SQSUtil.getQueueArn(sqsClient, url);
                declaredQueues.put(queueName, new QueueInfo(url, arn));
            }

            final var dlqName = queueName + SQSUtil.DEAD_LETTER_QUEUE_SUFFIX;
            final var dlqArn = createDeadLetterQueue(dlqName);
            addRedrivePolicy(declaredQueues.get(queueName).url(), dlqArn);
        }
        return declaredQueues.get(queueName);
    }

    private String createDeadLetterQueue(final String dlqName)
    {
        final var dlqAttributes = new HashMap<QueueAttributeName, String>();
        try {
            final var dlqCreateQueueRequest = CreateQueueRequest.builder()
                    .queueName(dlqName)
                    .attributes(dlqAttributes)
                    .build();

            final var dlqResponse = sqsClient.createQueue(dlqCreateQueueRequest);
            final var dlqUrl = dlqResponse.queueUrl();
            return SQSUtil.getQueueArn(sqsClient, dlqUrl);
        } catch (final QueueNameExistsException e) {
            LOG.info("Dead Letter Queue already exists {} {}", dlqName, e.getMessage());
            final var url = SQSUtil.getQueueUrl(sqsClient, dlqName);
            return SQSUtil.getQueueArn(sqsClient, url);
        }
    }

    private void addRedrivePolicy(final String sourceQueueUrl, final String dlqArn)
    {
        final var sourceAttributes = new HashMap<QueueAttributeName, String>();
        sourceAttributes.put(
                QueueAttributeName.REDRIVE_POLICY,
                String.format("{\"maxReceiveCount\":\"%d\", \"deadLetterTargetArn\":\"%s\"}",
                        sqsQueueConfiguration.getMaxDeliveries(), dlqArn)
        );

        var queueAttributesRequest = SetQueueAttributesRequest.builder()
                .queueUrl(sourceQueueUrl)
                .attributes(sourceAttributes)
                .build();
        sqsClient.setQueueAttributes(queueAttributesRequest);
    }

    private Map<QueueAttributeName, String> getAttributes()
    {
        final var attributes = new HashMap<QueueAttributeName, String>();
        attributes.put(
                QueueAttributeName.VISIBILITY_TIMEOUT,
                String.valueOf(sqsQueueConfiguration.getVisibilityTimeout())
        );

        attributes.put(
                QueueAttributeName.MESSAGE_RETENTION_PERIOD,
                String.valueOf(sqsQueueConfiguration.getMessageRetentionPeriod())
        );

        return attributes;
    }

    @Override
    public void publish(
            final TaskInformation taskInformation,
            final byte[] taskMessage,
            final String targetQueue,
            final Map<String, Object> headers, // DDD unused ?
            final boolean isLastMessage // DDD unused ?
    ) throws QueueException
    {
        try {
            // DDD what are we using the TaskInformation for here?
            final var queueUrl = SQSUtil.getQueueUrl(sqsClient, targetQueue);
            final var sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(new String(taskMessage, StandardCharsets.UTF_8))
                    .build();

            sqsClient.sendMessage(sendMsgRequest);
        } catch (final Exception e) {
            LOG.error("Error publishing task message {} {}", taskInformation.getInboundMessageId(), e.getMessage());
            throw new QueueException("Error publishing task message", e);
        }
    }

    @Override
    public void publish(
            final TaskInformation taskInformation,
            final byte[] taskMessage,
            final String targetQueue,
            final Map<String, Object> headers) throws QueueException
    {
        publish(taskInformation, taskMessage, targetQueue, headers, false);
    }

    /**
     *
     * @param taskInformation the queue task id that has been acknowledged
     */
    @Override
    public void acknowledgeTask(final TaskInformation taskInformation)
    {
        var sqsTaskInformation = (SQSTaskInformation)taskInformation;
        // DDD 2 Assumptions here:
        //  1. The same object passed to the callback is used to ack.
        //  2. Only acks for the defined input queue will get ack'd.
        final var deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(inputQueueUrl)
                .receiptHandle(sqsTaskInformation.getReceiptHandle())
                .build();
        sqsClient.deleteMessage(deleteRequest);
    }

    @Override
    public HealthResult healthCheck()
    {
        return null;
    }

    @Override
    public void shutdownIncoming()
    {

    }

    @Override
    public void shutdown()
    {

    }

    @Override
    public WorkerQueueMetricsReporter getMetrics()
    {
        return null;
    }

    @Override
    public void disconnectIncoming()
    {

    }

    @Override
    public void reconnectIncoming()
    {

    }

    @Override
    public void rejectTask(final TaskInformation taskInformation)
    {

    }

    @Override
    public void discardTask(final TaskInformation taskInformation)
    {

    }

    @Override
    public String getInputQueue()
    {
        return sqsQueueConfiguration.getInputQueue();
    }

    @Override
    public String getPausedQueue()
    {
        // DDD what here
        return "";
    }
}
