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

public abstract class QueueConsumer implements Runnable
{
    protected final SqsClient sqsClient;
    protected final QueueInfo queueInfo;
    protected final QueueInfo retryQueueInfo;
    protected final SQSWorkerQueueConfiguration queueCfg;
    protected final TaskCallback callback;
    protected final MetricsReporter metricsReporter;
    protected final VisibilityMonitor visibilityMonitor;
    protected final AtomicBoolean receiveMessages;
    protected final AtomicBoolean running = new AtomicBoolean(true);

    private static final Logger LOG = LoggerFactory.getLogger(QueueConsumer.class);

    public QueueConsumer(
            final SqsClient sqsClient,
            final QueueInfo queueInfo,
            final QueueInfo retryQueueInfo,
            final SQSWorkerQueueConfiguration queueCfg,
            final TaskCallback callback,
            final VisibilityMonitor visibilityMonitor,
            final MetricsReporter metricsReporter,
            final AtomicBoolean receiveMessages)
    {
        this.sqsClient = sqsClient;
        this.queueInfo = queueInfo;
        this.retryQueueInfo = retryQueueInfo;
        this.queueCfg = queueCfg;
        this.callback = callback;
        this.metricsReporter = metricsReporter;
        this.visibilityMonitor = visibilityMonitor;
        this.receiveMessages = receiveMessages;
    }

    @Override
    public void run()
    {
        final var receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueInfo.url())
                .maxNumberOfMessages(queueCfg.getMaxNumberOfMessages())
                .waitTimeSeconds(queueCfg.getLongPollInterval())
                .messageSystemAttributeNames(MessageSystemAttributeName.ALL)
                .messageAttributeNames(SQSUtil.ALL_ATTRIBUTES)
                .build();
        while (running.get()) {
            if (receiveMessages.get()) {
                receiveMessages(receiveRequest);
            } else {
                // DDD should we be doing this
                Thread.onSpinWait();
            }
        }
    }

    protected void receiveMessages(final ReceiveMessageRequest receiveRequest)
    {
        final var receiveMessageResult = sqsClient.receiveMessage(receiveRequest).messages();
        if (!receiveMessageResult.isEmpty()) {
            LOG.debug("Received {} messages from queue {} \n{}",
                    receiveMessageResult.size(),
                    queueInfo.name(),
                    receiveMessageResult.stream().map(Message::receiptHandle).collect(Collectors.toSet()));
        }

        for(final var message : receiveMessageResult) {
            registerNewTask(message);
        }
    }

    protected void retryMessage(final Message message)
    {
        try {
            if (retryQueueInfo.equals(queueInfo)) {
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

    protected void registerNewTask(final Message message)
    {
        metricsReporter.incrementReceived();
        final var becomesVisible = Instant.now().getEpochSecond() + queueCfg.getVisibilityTimeout();
        final var taskInfo = new SQSTaskInformation(
                message.messageId(),
                new VisibilityTimeout(queueInfo, becomesVisible, message.receiptHandle()),
                isPoisonMessageConsumer()
        );

        final var headers = createHeadersFromMessageAttributes(message);
        try {
            callback.registerNewTask(taskInfo, message.body().getBytes(StandardCharsets.UTF_8), headers);
            handleConsumerSpecificActions(taskInfo);
        } catch (final InvalidTaskException e) {
            LOG.error("Cannot register new message, rejecting {}", taskInfo, e);
            retryMessage(message); // DDD note not yet watched
        } catch (final TaskRejectedException e) {
            metricsReporter.incrementRejected(); // DDD note not yet watched
            LOG.warn("Message {} rejected as a task at this time, will be redelivered by SQS",
                    taskInfo, e);
        }
    }

    public void shutdown()
    {
        running.set(false);
    }

    protected abstract void handleConsumerSpecificActions(final SQSTaskInformation taskInfo);

    protected abstract boolean isPoisonMessageConsumer();
}
