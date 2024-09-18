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
package com.hpe.caf.worker.queue.sqs.consumer;

import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.worker.queue.sqs.QueueInfo;
import com.hpe.caf.worker.queue.sqs.SQSTaskInformation;
import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;
import com.hpe.caf.worker.queue.sqs.metrics.MetricsReporter;
import com.hpe.caf.worker.queue.sqs.util.SQSUtil;
import com.hpe.caf.worker.queue.sqs.visibility.VisibilityMonitor;
import com.hpe.caf.worker.queue.sqs.visibility.VisibilityTimeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class QueueConsumer implements Runnable
{
    protected final SqsClient sqsClient;
    protected final QueueInfo inputQueueInfo;
    protected final QueueInfo deadLetterQueueInfo;
    protected final QueueInfo retryQueueInfo;
    protected final SQSWorkerQueueConfiguration queueCfg;
    protected final TaskCallback callback;
    protected final MetricsReporter metricsReporter;
    protected final VisibilityMonitor visibilityMonitor;
    protected final AtomicBoolean receiveMessages;
    private final int maxTasks;
    protected final AtomicBoolean running = new AtomicBoolean(true);

    private static final Logger LOG = LoggerFactory.getLogger(QueueConsumer.class);

    public QueueConsumer(
            final SqsClient sqsClient,
            final QueueInfo inputQueueInfo,
            final QueueInfo deadLetterQueueInfo,
            final QueueInfo retryQueueInfo,
            final TaskCallback callback,
            final SQSWorkerQueueConfiguration queueCfg,
            final VisibilityMonitor visibilityMonitor,
            final MetricsReporter metricsReporter,
            final AtomicBoolean receiveMessages,
            final int maxTasks)
    {
        this.sqsClient = sqsClient;
        this.inputQueueInfo = inputQueueInfo;
        this.deadLetterQueueInfo = deadLetterQueueInfo;
        this.retryQueueInfo = retryQueueInfo;
        this.queueCfg = queueCfg;
        this.callback = callback;
        this.metricsReporter = metricsReporter;
        this.visibilityMonitor = visibilityMonitor;
        this.receiveMessages = receiveMessages;
        this.maxTasks = maxTasks;
    }

    @Override
    public void run()
    {
        final var batchSize = getReceiveBatchSize();
        final var inputQueueRequest = ReceiveMessageRequest.builder()
                .queueUrl(inputQueueInfo.url())
                .maxNumberOfMessages(batchSize)
                .waitTimeSeconds(queueCfg.getLongPollInterval())
                .messageSystemAttributeNames(MessageSystemAttributeName.ALL)
                .messageAttributeNames(SQSUtil.ALL_ATTRIBUTES)
                .build();

        final var deadLetterQueueRequest = ReceiveMessageRequest.builder()
                .queueUrl(deadLetterQueueInfo.url())
                .maxNumberOfMessages(batchSize)
                .waitTimeSeconds(0) // should this have a different wait, given low expectation of messages
                .messageSystemAttributeNames(MessageSystemAttributeName.ALL)
                .messageAttributeNames(SQSUtil.ALL_ATTRIBUTES)
                .build();

        try {
            while (running.get()) {
                if (receiveMessages.get() &&
                    visibilityMonitor.hasInflightCapacity(inputQueueInfo, queueCfg, batchSize) &&
                    visibilityMonitor.hasInflightCapacity(deadLetterQueueInfo, queueCfg, batchSize)
                ) {
                    receiveMessages(inputQueueRequest, false);
                    receiveMessages(deadLetterQueueRequest, true);
                } else {
                    try {
                        Thread.sleep(queueCfg.getLongPollInterval() * 1000);
                    } catch (final InterruptedException e) {
                        LOG.error("A pause in task deletion was interrupted", e);
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("A pause in task deletion was interrupted", e);
        }
    }

    protected void receiveMessages(
            final ReceiveMessageRequest receiveRequest,
            final boolean isPoisonMessageConsumer
    )
    {
        final var receiveMessageResult = sqsClient.receiveMessage(receiveRequest).messages();
        if (!receiveMessageResult.isEmpty()) {
            LOG.debug("Received {} messages from queue {} \n{}",
                    receiveMessageResult.size(),
                    inputQueueInfo.name(),
                    receiveMessageResult.stream().map(Message::receiptHandle).collect(Collectors.toSet()));
        }

        for(final var message : receiveMessageResult) {
            registerNewTask(message, isPoisonMessageConsumer);
        }
    }

    protected void retryMessage(final Message message)
    {
        try {
            if (retryQueueInfo.equals(inputQueueInfo)) {
               return;
            }

            final var attributes = new HashMap<>(message.messageAttributes());
            attributes.put(SQSUtil.SQS_HEADER_CAF_WORKER_REJECTED, MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(SQSUtil.REJECTED_REASON_TASKMESSAGE)
                    .build());
            final var request = SendMessageRequest.builder()
                    .queueUrl(retryQueueInfo.url())
                    .messageBody(message.body())
                    .messageAttributes(attributes)
                    .build();
            sqsClient.sendMessage(request);
        } catch (final Exception e) {
            metricsReporter.incrementErrors();
            var msg = String.format("Error sending message to retry queue:%s messageId:%s",
                    retryQueueInfo.name(), message.messageId());
            LOG.error(msg, e);
        }
    }

    protected Map<String, Object> createHeadersFromMessageAttributes(final Message message)
    {
        final var headers = new HashMap<String, Object>();
        for(final Map.Entry<String, MessageAttributeValue> entry : message.messageAttributes().entrySet()) {
            if (entry.getValue().dataType().equals("String")) {
                headers.put(entry.getKey(), entry.getValue().stringValue());
            }
        }
        return headers;
    }

    protected void registerNewTask(final Message message, final boolean isPoisonMessageConsumer)
    {
        metricsReporter.incrementReceived();
        final var becomesVisible = Instant.now().getEpochSecond() + queueCfg.getVisibilityTimeout();
        final var taskInfo = new SQSTaskInformation(
                message.messageId(),
                new VisibilityTimeout(inputQueueInfo, becomesVisible, message.receiptHandle()),
                isPoisonMessageConsumer
        );

        final var headers = createHeadersFromMessageAttributes(message);
        try {
            callback.registerNewTask(taskInfo, message.body().getBytes(StandardCharsets.UTF_8), headers);
            visibilityMonitor.watch(taskInfo);
        } catch (final InvalidTaskException e) {
            LOG.error("Cannot register new message, rejecting {}", taskInfo, e);
            retryMessage(message);
        } catch (final TaskRejectedException e) {
            metricsReporter.incrementRejected();
            LOG.warn("Message {} rejected as a task at this time, will be redelivered by SQS",
                    taskInfo, e);
        }
    }

    public void shutdown()
    {
        running.set(false);
    }

    private int getReceiveBatchSize()
    {
        if (maxTasks > SQSUtil.MAX_MESSAGE_BATCH_SIZE ||
                queueCfg.getMaxNumberOfMessages() + maxTasks > SQSUtil.MAX_MESSAGE_BATCH_SIZE) {
            LOG.debug("Calculated receive batch size: {}", SQSUtil.MAX_MESSAGE_BATCH_SIZE);
            return SQSUtil.MAX_MESSAGE_BATCH_SIZE;
        }

        var batchSize = Math.max(queueCfg.getMaxNumberOfMessages(), maxTasks);
        if (queueCfg.getMaxNumberOfMessages() + maxTasks <= SQSUtil.MAX_MESSAGE_BATCH_SIZE) {
            batchSize = queueCfg.getMaxNumberOfMessages() + maxTasks;
        }

        LOG.debug("Calculated receive batch size: {}", batchSize);
        return batchSize;
    }
}
