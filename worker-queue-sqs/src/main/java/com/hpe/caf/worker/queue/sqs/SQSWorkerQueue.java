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
import com.hpe.caf.worker.queue.sqs.consumer.QueueConsumer;
import com.hpe.caf.worker.queue.sqs.metrics.MetricsReporter;
import com.hpe.caf.worker.queue.sqs.publisher.DeletePublisher;
import com.hpe.caf.worker.queue.sqs.publisher.WorkerPublisher;
import com.hpe.caf.worker.queue.sqs.publisher.message.DeleteMessage;
import com.hpe.caf.worker.queue.sqs.publisher.message.WorkerMessage;
import com.hpe.caf.worker.queue.sqs.util.DeadLetteredQueuePair;
import com.hpe.caf.worker.queue.sqs.util.SQSUtil;
import com.hpe.caf.worker.queue.sqs.visibility.VisibilityMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SQSWorkerQueue implements ManagedWorkerQueue
{
    private SqsClient sqsClient;

    private Thread consumerThread;
    private Thread visibilityMonitorThread;
    private Thread deletePublisherThread;
    private Thread workerPublisherThread;

    private VisibilityMonitor visibilityMonitor;
    private QueueConsumer consumer;
    private DeletePublisher deletePublisher;
    private WorkerPublisher workerPublisher;

    private final AtomicBoolean receiveMessages;
    private final int maxTasks;
    private final MetricsReporter metricsReporter;
    private final SQSWorkerQueueConfiguration queueCfg;
    private final Map<String, QueueInfo> declaredQueues = new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(SQSWorkerQueue.class);

    public SQSWorkerQueue(
            final SQSWorkerQueueConfiguration queueCfg,
            int maxTasks
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

            final var queuePair = createDeadLetteredQueuePair(queueCfg.getInputQueue());

            final var inputQueueInfo = queuePair.queue();
            LOG.debug("inputQueueInfo {}", inputQueueInfo);
            final var deadLetterQueueInfo = queuePair.deadLetterQueue();
            LOG.debug("deadLetterQueueInfo {}", deadLetterQueueInfo);

            final var retryQueueInfo = createDeadLetteredQueuePair(queueCfg.getRetryQueue()).queue();
            createDeadLetteredQueuePair(queueCfg.getRejectedQueue());

            visibilityMonitor = new VisibilityMonitor(
                    sqsClient,
                    queueCfg.getVisibilityTimeout());

            consumer = new QueueConsumer(
                    sqsClient,
                    inputQueueInfo,
                    deadLetterQueueInfo,
                    retryQueueInfo,
                    callback,
                    queueCfg,
                    visibilityMonitor,
                    metricsReporter,
                    receiveMessages,
                    maxTasks);

            deletePublisher = new DeletePublisher(sqsClient, visibilityMonitor);

            workerPublisher = new WorkerPublisher(sqsClient, queueCfg, visibilityMonitor);

            workerPublisherThread = new Thread(workerPublisher);
            deletePublisherThread = new Thread(deletePublisher);
            visibilityMonitorThread = new Thread(visibilityMonitor);
            consumerThread = new Thread(consumer);

            workerPublisherThread.start();
            deletePublisherThread.start();
            visibilityMonitorThread.start();
            consumerThread.start();
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
            final boolean isLastMessage
    ) throws QueueException
    {
        var sqsTaskInformation = (SQSTaskInformation) taskInformation;
        try {
            final var queueInfo = createDeadLetteredQueuePair(targetQueue).queue();
            workerPublisher.publish(new WorkerMessage(queueInfo, taskMessage, headers, sqsTaskInformation));
            LOG.debug("Queued for publishing {}", sqsTaskInformation.getReceiptHandle());
            sqsTaskInformation.incrementResponseCount(isLastMessage);
            if (sqsTaskInformation.processingComplete()) {
                deletePublisher.publish(new DeleteMessage(sqsTaskInformation)); // enables batching
            }
        } catch (final Exception e) {
            metricsReporter.incrementErrors();
            LOG.error("Error publishing task message {} {}", sqsTaskInformation, e.getMessage());
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

    @Override
    public void acknowledgeTask(final TaskInformation taskInformation)
    {
        var sqsTaskInformation = (SQSTaskInformation) taskInformation;
        sqsTaskInformation.incrementAcknowledgementCount();
        if (sqsTaskInformation.processingComplete()) {
            deletePublisher.publish(new DeleteMessage(sqsTaskInformation)); // enables batching
        }
    }

    @Override
    public HealthResult livenessCheck()
    {
        if (isNotRunning(consumerThread)) {
            return new HealthResult(HealthStatus.UNHEALTHY, "SQS consumer thread state:" +
                    getState(consumerThread));
        } else if (isNotRunning(visibilityMonitorThread))  {
            return new HealthResult(HealthStatus.UNHEALTHY, "SQS visibility monitor thread state:" +
                    getState(visibilityMonitorThread));
        } else if (isNotRunning(deletePublisherThread))  {
            return new HealthResult(HealthStatus.UNHEALTHY, "SQS delete message thread state:" +
                    getState(deletePublisherThread));
        } else if (isNotRunning(workerPublisherThread))  {
            return new HealthResult(HealthStatus.UNHEALTHY, "SQS worker publisher thread state:" +
                    getState(workerPublisherThread));
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
        visibilityMonitor.shutdown();
        deletePublisher.shutdown();
        workerPublisher.shutdown();
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

    private DeadLetteredQueuePair createDeadLetteredQueuePair(final String queueName)
    {
        final var queue = declaredQueues.computeIfAbsent(
                queueName,
                (q) -> SQSUtil.createQueue(sqsClient, q, queueCfg)
        );

        final var dlqueue = declaredQueues.computeIfAbsent(
                queueName + SQSUtil.DEAD_LETTER_QUEUE_SUFFIX,
                (q) -> SQSUtil.createDeadLetterQueue(sqsClient, queue, queueCfg)
        );
        return new DeadLetteredQueuePair(queue, dlqueue);
    }

    private static boolean isNotRunning(final Thread t)
    {
        return (t == null) || t.getState() == Thread.State.NEW || t.getState() == Thread.State.TERMINATED;
    }

    private static String getState(final Thread t)
    {
        return t == null ? "NULL" : t.getState().name();
    }
}
