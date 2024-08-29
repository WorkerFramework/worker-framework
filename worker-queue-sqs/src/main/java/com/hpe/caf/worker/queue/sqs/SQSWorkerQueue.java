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
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.TaskInformation;
import com.hpe.caf.api.worker.WorkerQueueMetricsReporter;
import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;
import com.hpe.caf.worker.queue.sqs.consumer.DeadLetterQueueConsumer;
import com.hpe.caf.worker.queue.sqs.consumer.InputQueueConsumer;
import com.hpe.caf.worker.queue.sqs.visibility.VisibilityMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.ReceiptHandleIsInvalidException;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.hpe.caf.worker.queue.sqs.SQSUtil.getDeadLetterQueueAttributes;
import static com.hpe.caf.worker.queue.sqs.SQSUtil.getInputQueueAttributes;

public final class SQSWorkerQueue implements ManagedWorkerQueue
{
    private SqsClient sqsClient;
    private Thread inputQueueConsumerThread;
    private Thread deadLetterQueueConsumerThread;
    private Thread visibilityMonitorThread;
    private VisibilityMonitor visibilityMonitor;

    private final SQSWorkerQueueConfiguration sqsQueueCfg;
    private final SQSClientProvider clientProvider;
    private final Map<String, QueueInfo> declaredQueues = new ConcurrentHashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(SQSWorkerQueue.class);


    public SQSWorkerQueue(
            final SQSWorkerQueueConfiguration sqsQueueCfg
    )
    {
        this.sqsQueueCfg = Objects.requireNonNull(sqsQueueCfg);
        clientProvider = new SQSClientProviderImpl(sqsQueueCfg.getSQSConfiguration());
    }

    public void start(final TaskCallback callback) throws QueueException
    {
        if (sqsClient != null) {
            throw new IllegalStateException("Already started");
        }
        try {
            sqsClient = clientProvider.getSqsClient();
            final var inputQueueInfo = declaredQueues.computeIfAbsent(
                    sqsQueueCfg.getInputQueue(),
                    (q) -> createQueue(q, getInputQueueAttributes(sqsQueueCfg))
            );

            final var deadLetterQueueInfo = declaredQueues.computeIfAbsent(
                    sqsQueueCfg.getInputQueue() + SQSUtil.DEAD_LETTER_QUEUE_SUFFIX,
                    (q) -> createQueue(q, getDeadLetterQueueAttributes(sqsQueueCfg))
            );
            addRedrivePolicy(inputQueueInfo.url(), deadLetterQueueInfo.arn());

            final var retryQueueInfo = declaredQueues.computeIfAbsent(
                    sqsQueueCfg.getRetryQueue(),
                    (q) -> createQueue(q, getInputQueueAttributes(sqsQueueCfg))
            );

            visibilityMonitor = new VisibilityMonitor(
                    sqsClient,
                    inputQueueInfo.url(),
                    sqsQueueCfg.getVisibilityTimeout());

            var dlqConsumer = new DeadLetterQueueConsumer(
                    sqsClient,
                    deadLetterQueueInfo,
                    retryQueueInfo,
                    callback,
                    sqsQueueCfg);

            var consumer = new InputQueueConsumer(
                    sqsClient,
                    inputQueueInfo,
                    retryQueueInfo,
                    callback,
                    sqsQueueCfg,
                    visibilityMonitor);

            visibilityMonitorThread = new Thread(visibilityMonitor);
            visibilityMonitorThread.start();

            inputQueueConsumerThread = new Thread(consumer);
            inputQueueConsumerThread.start();

            deadLetterQueueConsumerThread = new Thread(dlqConsumer);
            deadLetterQueueConsumerThread.start();
        } catch (final Exception e) {
            throw new QueueException("Failed to start worker queue", e);
        }
    }

    /**
     * @param queueName The name of the queue.
     * @return An object containing the name,url and arn of the queue.
     */
    private QueueInfo createQueue(final String queueName, final Map<QueueAttributeName, String> attributes)
    {
        try {
            final var createQueueRequest = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .attributes(attributes)
                    .build();

            final var response = sqsClient.createQueue(createQueueRequest);
            final var url = response.queueUrl();
            final var arn = SQSUtil.getQueueArn(sqsClient, url);
            return new QueueInfo(queueName, url, arn);
        } catch (final QueueNameExistsException e) {
            LOG.info("Queue already exists {} {}", queueName, e.getMessage());
            var url = SQSUtil.getQueueUrl(sqsClient, queueName);
            final var arn = SQSUtil.getQueueArn(sqsClient, url);
            return new QueueInfo(queueName, url, arn);
        }
    }

    private void addRedrivePolicy(
            final String inputQueueUrl,
            final String deadLetterQueueArn)
    {
        final var sourceAttributes = new HashMap<QueueAttributeName, String>();
        sourceAttributes.put(
                QueueAttributeName.REDRIVE_POLICY,
                String.format("{\"maxReceiveCount\":\"%d\", \"deadLetterTargetArn\":\"%s\"}",
                        sqsQueueCfg.getMaxDeliveries(), deadLetterQueueArn)
        );

        var queueAttributesRequest = SetQueueAttributesRequest.builder()
                .queueUrl(inputQueueUrl)
                .attributes(sourceAttributes)
                .build();
        sqsClient.setQueueAttributes(queueAttributesRequest);
    }

    @Override
    // DDD this can go on publish queue so it can be retried??
    // Then it could possibly publish in batches
    public void publish(
            final TaskInformation taskInformation,
            final byte[] taskMessage,
            final String targetQueue,
            final Map<String, Object> headers,
            final boolean isLastMessage // DDD unused ?
    ) throws QueueException
    {
        try {
            // DDD what are we using the TaskInformation for here?
            final var queueInfo = declaredQueues.computeIfAbsent(
                    targetQueue,
                    (q) -> SQSUtil.getQueueInfo(sqsClient, targetQueue)
            );

            var attributes = createAttributesFromMessageHeaders(headers);

            final var sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(queueInfo.url())
                    .messageBody(new String(taskMessage, StandardCharsets.UTF_8))
                    .messageAttributes(attributes)
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
     * @param taskInformation The object containing metadata about a queued message.
     */
    @Override
    public void acknowledgeTask(final TaskInformation taskInformation)
    {
        var sqsTaskInformation = (SQSTaskInformation) taskInformation;
        try {
            // DDD or write to blocking queue so we can batch deletes
            // and remove from visibility monitor at same time.
            // having task extended till then has no side effect.
            final var deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(sqsTaskInformation.getQueueInfo().url())
                    .receiptHandle(sqsTaskInformation.getReceiptHandle())
                    .build();
            sqsClient.deleteMessage(deleteRequest);
        } catch (final ReceiptHandleIsInvalidException e) {
            LOG.error("Receipt handle: {} is invalid - messageId:{}. {}",
                    sqsTaskInformation.getVisibilityTimeout().receiptHandle(),
                    sqsTaskInformation.getInboundMessageId(),
                    e.getMessage());
        } catch (final QueueDoesNotExistException e) {
            LOG.error("Queue {} may have been deleted. {}",
                    sqsTaskInformation.getQueueInfo().name(),
                    e.getMessage());
        } finally {
            visibilityMonitor.unwatch(sqsTaskInformation);
        }
    }

    @Override
    public HealthResult livenessCheck()
    {
        if (inputQueueConsumerThread == null || !inputQueueConsumerThread.isAlive()) {
            return new HealthResult(HealthStatus.UNHEALTHY, "SQS input queue thread not running");
        } else if (deadLetterQueueConsumerThread == null || !deadLetterQueueConsumerThread.isAlive()) {
            return new HealthResult(HealthStatus.UNHEALTHY, "SQS dead letter queue thread not running");
        } else if (visibilityMonitorThread == null || !visibilityMonitorThread.isAlive()) {
            return new HealthResult(HealthStatus.UNHEALTHY, "Visibility monitor thread thread not running");
        } else {
            return HealthResult.RESULT_HEALTHY;
        }
    }

    @Override
    public HealthResult healthCheck()
    {
        return livenessCheck();
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
        // DDD
    }

    @Override
    public void reconnectIncoming()
    {
        // DDD
    }

    @Override
    public void rejectTask(final TaskInformation taskInformation)
    {
        // DDD delete/redeliver/move?
        var sqsTaskInformation = (SQSTaskInformation) taskInformation;
        LOG.debug("About to unwatch rejected task {}", sqsTaskInformation.getReceiptHandle());
        visibilityMonitor.unwatch(sqsTaskInformation);
    }

    @Override
    public void discardTask(final TaskInformation taskInformation)
    {
        // DDD delete/redeliver/move?
        var sqsTaskInformation = (SQSTaskInformation) taskInformation;
        LOG.debug("About to unwatch discarded task {}", sqsTaskInformation.getReceiptHandle());
        visibilityMonitor.unwatch(sqsTaskInformation);
    }

    @Override
    public String getInputQueue()
    {
        return sqsQueueCfg.getInputQueue();
    }

    @Override
    public String getPausedQueue()
    {
        // DDD what here, seems to be unused/deprecated in rabbit impl
        return "";
    }

    private Map<String, MessageAttributeValue> createAttributesFromMessageHeaders(final Map<String, Object> headers)
    {
        final var attributes = new HashMap<String, MessageAttributeValue>();
        for(final Map.Entry<String, Object> entry : headers.entrySet()) {
            attributes.put(entry.getKey(), MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(entry.getValue().toString())
                    .build());
        }
        return attributes;
    }
}
