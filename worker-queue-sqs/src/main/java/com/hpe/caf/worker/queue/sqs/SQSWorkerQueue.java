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
import com.hpe.caf.api.worker.*;
import com.hpe.caf.configs.SQSConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class SQSWorkerQueue implements ManagedWorkerQueue
{
    private SqsClient sqsClient;
    private Thread inputQueueConsumerThread;
    private Thread deadLetterQueueConsumerThread;
    private QueueInfo inputQueueInfo;
    private QueueInfo deadLetterQueueInfo;
    private TaskCallback callback;

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
        this.callback = callback;
        if (sqsClient != null) {
            throw new IllegalStateException("Already started");
        }
        try {
            sqsClient = clientProvider.getSqsClient(sqsConfiguration);
            inputQueueInfo = createQueue(sqsQueueConfiguration.getInputQueue(), getInputQueueAttributes());

            final var dlqName = sqsQueueConfiguration.getInputQueue() + SQSUtil.DEAD_LETTER_QUEUE_SUFFIX;
            deadLetterQueueInfo = createQueue(dlqName, getDeadLetterQueueAttributes());
            addRedrivePolicy(inputQueueInfo.url(), deadLetterQueueInfo.arn());

            var dlqConsumer = new SQSMessageConsumer(
                    sqsClient, deadLetterQueueInfo, callback, sqsQueueConfiguration, true);

            var consumer = new SQSMessageConsumer(
                    sqsClient, inputQueueInfo, callback, sqsQueueConfiguration, false);

            inputQueueConsumerThread = new Thread(consumer);
            inputQueueConsumerThread.start();

            deadLetterQueueConsumerThread = new Thread(dlqConsumer);
            deadLetterQueueConsumerThread.start();
        } catch (final Exception e) {
            throw new QueueException("Failed to start worker queue", e);
        }
    }

    /**
     * @param queueName
     * @return
     */
    private QueueInfo createQueue(final String queueName, final Map<QueueAttributeName, String> attributes)
    {
        if (!declaredQueues.containsKey(queueName)) {
            try {
                final var createQueueRequest = CreateQueueRequest.builder()
                        .queueName(queueName)
                        .attributes(attributes)
                        .build();

                final var response = sqsClient.createQueue(createQueueRequest);
                final var url = response.queueUrl();
                final var arn = SQSUtil.getQueueArn(sqsClient, url);
                declaredQueues.put(queueName, new QueueInfo(queueName, url, arn));
            } catch (final QueueNameExistsException e) {
                LOG.info("Queue already exists {} {}", queueName, e.getMessage());
                var url = SQSUtil.getQueueUrl(sqsClient, queueName);
                final var arn = SQSUtil.getQueueArn(sqsClient, url);
                declaredQueues.put(queueName, new QueueInfo(queueName, url, arn));
            }
        }
        return declaredQueues.get(queueName);
    }

    private void addRedrivePolicy(
            final String inputQueueUrl,
            final String deadLetterQueueArn)
    {
        final var sourceAttributes = new HashMap<QueueAttributeName, String>();
        sourceAttributes.put(
                QueueAttributeName.REDRIVE_POLICY,
                String.format("{\"maxReceiveCount\":\"%d\", \"deadLetterTargetArn\":\"%s\"}",
                        sqsQueueConfiguration.getMaxDeliveries(), deadLetterQueueArn)
        );

        var queueAttributesRequest = SetQueueAttributesRequest.builder()
                .queueUrl(inputQueueUrl)
                .attributes(sourceAttributes)
                .build();
        sqsClient.setQueueAttributes(queueAttributesRequest);
    }

    private Map<QueueAttributeName, String> getInputQueueAttributes()
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

    private Map<QueueAttributeName, String> getDeadLetterQueueAttributes()
    {
        final var attributes = new HashMap<QueueAttributeName, String>();
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
     * @param taskInformation
     */
    @Override
    public void acknowledgeTask(final TaskInformation taskInformation)
    {
        var sqsTaskInformation = (SQSTaskInformation)taskInformation;
        try {

            final var deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(sqsTaskInformation.getQueueInfo().url())
                    .receiptHandle(sqsTaskInformation.getReceiptHandle())
                    .build();
            sqsClient.deleteMessage(deleteRequest);
        } catch (final ReceiptHandleIsInvalidException e) {
            LOG.info("Receipt handle: {} has expired - messageId:{}. {}",
                    sqsTaskInformation.getReceiptHandle(),
                    sqsTaskInformation.getInboundMessageId(),
                    e.getMessage());
        } catch (final QueueDoesNotExistException e) {
            LOG.info("Queue {} may have been deleted. {}",
                    sqsTaskInformation.getQueueInfo().name(),
                    e.getMessage());
        }
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
