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
import com.hpe.caf.worker.queue.sqs.metrics.MetricsReporter;
import com.hpe.caf.worker.queue.sqs.util.QueuePair;
import com.hpe.caf.worker.queue.sqs.util.SQSUtil;
import com.hpe.caf.worker.queue.sqs.visibility.VisibilityMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiptHandleIsInvalidException;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SQSWorkerQueue implements ManagedWorkerQueue
{
    private SqsClient sqsClient;

    private Thread inputQueueThread;
    private Thread deadLetterQueueThread;
    private Thread visibilityMonitorThread;

    private VisibilityMonitor visibilityMonitor;
    private InputQueueConsumer consumer;
    private DeadLetterQueueConsumer dlqConsumer;

    private final AtomicBoolean receiveMessages;
    private final int maxTasks;
    private final MetricsReporter metricsReporter;
    private final SQSWorkerQueueConfiguration queueCfg;
    private final Map<String, QueueInfo> declaredQueues = new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(SQSWorkerQueue.class);

    public SQSWorkerQueue(
            final SQSWorkerQueueConfiguration queueCfg,
            int maxTasks // Add to max messages
    )
    {
        this.maxTasks = maxTasks;
        this.queueCfg = Objects.requireNonNull(queueCfg);
        metricsReporter = new MetricsReporter();
        receiveMessages = new AtomicBoolean(true);
    }

    public void start(final TaskCallback callback) throws QueueException
    {
        if (sqsClient != null) {
            throw new IllegalStateException("Already started");
        }
        try {
            sqsClient = SQSUtil.getSqsClient(queueCfg.getSqsConfiguration());

            final var queuePair = createQueuePair(queueCfg.getInputQueue());

            final var inputQueueInfo = queuePair.queue();
            final var deadLetterQueueInfo = queuePair.deadLetterQueue();

            final var retryQueueInfo = declaredQueues.computeIfAbsent(
                    queueCfg.getRetryQueue(),
                    (q) -> SQSUtil.createQueue(sqsClient, q, queueCfg)
            );

            visibilityMonitor = new VisibilityMonitor(
                    sqsClient,
                    queueCfg.getVisibilityTimeout());

            dlqConsumer = new DeadLetterQueueConsumer(
                    sqsClient,
                    deadLetterQueueInfo,
                    callback,
                    queueCfg,
                    visibilityMonitor,
                    metricsReporter,
                    receiveMessages,
                    maxTasks);

            consumer = new InputQueueConsumer(
                    sqsClient,
                    inputQueueInfo,
                    retryQueueInfo,
                    callback,
                    queueCfg,
                    visibilityMonitor,
                    metricsReporter,
                    receiveMessages,
                    maxTasks);

            visibilityMonitorThread = new Thread(visibilityMonitor);
            inputQueueThread = new Thread(consumer);
            deadLetterQueueThread = new Thread(dlqConsumer);

            visibilityMonitorThread.start();
            inputQueueThread.start();
            deadLetterQueueThread.start();
        } catch (final Exception e) {
            throw new QueueException("Failed to start worker queue", e);
        }
    }

    public boolean isReceiving()
    {
        return receiveMessages.get();
    }

    @Override
    public void publish(
            final TaskInformation taskInformation,
            final byte[] taskMessage,
            final String targetQueue,
            final Map<String, Object> headers,
            final boolean isLastMessage // DDD unused ?
    ) throws QueueException
    {
        try {
            final var queueInfo = declaredQueues.computeIfAbsent(
                    targetQueue,
                    (q) -> createQueuePair(targetQueue).queue()
            );

            var attributes = createAttributesFromMessageHeaders(headers);

            final var sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(queueInfo.url())
                    .messageBody(new String(taskMessage, StandardCharsets.UTF_8))
                    .messageAttributes(attributes)
                    .build();
            sqsClient.sendMessage(sendMsgRequest);
        } catch (final Exception e) {
            metricsReporter.incrementErrors();
            LOG.error("Error publishing task message {} {}", taskInformation, e.getMessage());
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
            final var deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(sqsTaskInformation.getQueueInfo().url())
                    .receiptHandle(sqsTaskInformation.getReceiptHandle())
                    .build();
            sqsClient.deleteMessage(deleteRequest);
        } catch (final ReceiptHandleIsInvalidException e) {
            LOG.error("TaskInformation is invalid:{}. {}",
                    sqsTaskInformation,
                    e.getMessage());
            metricsReporter.incrementErrors();
        } catch (final QueueDoesNotExistException e) {
            LOG.error("Queue {} may have been deleted. {}",
                    sqsTaskInformation.getQueueInfo().name(),
                    e.getMessage());
            metricsReporter.incrementErrors();
        } finally {
            visibilityMonitor.unwatch(sqsTaskInformation);
        }
    }

    @Override
    public HealthResult livenessCheck()
    {
        if (isNotRunning(inputQueueThread)) {
            return new HealthResult(HealthStatus.UNHEALTHY, "SQS input queue thread not running");
        } else if (isNotRunning(deadLetterQueueThread)) {
            return new HealthResult(HealthStatus.UNHEALTHY, "SQS dead letter queue thread not running");
        } else if (isNotRunning(visibilityMonitorThread))  {
            return new HealthResult(HealthStatus.UNHEALTHY, "SQS visibility monitor thread not running");
        }
        return HealthResult.RESULT_HEALTHY;
    }

    @Override
    public HealthResult healthCheck()
    {
        return livenessCheck();
    }

    @Override
    public void shutdownIncoming()
    {
        receiveMessages.set(false);
    }

    @Override
    public void shutdown()
    {
        consumer.shutdown();
        dlqConsumer.shutdown();
        visibilityMonitor.shutdown();
    }

    @Override
    public void disconnectIncoming()
    {
        receiveMessages.set(false);
    }

    @Override
    public void reconnectIncoming()
    {
        receiveMessages.set(true);
    }

    @Override
    public WorkerQueueMetricsReporter getMetrics()
    {
        return metricsReporter;
    }

    @Override
    public void rejectTask(final TaskInformation taskInformation)
    {
        metricsReporter.incrementRejected();
        final var sqsTaskInformation = (SQSTaskInformation) taskInformation;
        LOG.debug("About to unwatch rejected task {}", sqsTaskInformation.getReceiptHandle());
        visibilityMonitor.unwatch(sqsTaskInformation); // forces redelivery
    }

    @Override
    public void discardTask(final TaskInformation taskInformation)
    {
        metricsReporter.incrementDropped();
        final var sqsTaskInformation = (SQSTaskInformation) taskInformation;
        LOG.debug("About to unwatch discarded task {}", sqsTaskInformation.getReceiptHandle());
        visibilityMonitor.unwatch(sqsTaskInformation); // forces redelivery
    }

    @Override
    public String getInputQueue()
    {
        return queueCfg.getInputQueue();
    }

    @Override
    public String getPausedQueue()
    {
        return queueCfg.getPausedQueue();
    }

    private QueuePair createQueuePair(final String queueName)
    {
        final var queue = declaredQueues.computeIfAbsent(
                queueName,
                (q) -> SQSUtil.createQueue(sqsClient, q, queueCfg)
        );

        final var dlqueue = declaredQueues.computeIfAbsent(
                queueName + SQSUtil.DEAD_LETTER_QUEUE_SUFFIX,
                (q) -> SQSUtil.createDeadLetterQueue(sqsClient, queue, queueCfg)
        );
        return new QueuePair(queue, dlqueue);
    }

    private static Map<String, MessageAttributeValue> createAttributesFromMessageHeaders(final Map<String, Object> headers)
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

    private static boolean isNotRunning(final Thread t)
    {
        // If the thread was not started or is terminated
        return t == null || t.getState().equals(Thread.State.NEW) || t.getState().equals(Thread.State.TERMINATED);
    }
}
