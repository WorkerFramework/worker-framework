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

import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SQSMessageConsumer implements Runnable
{
    private final SqsClient sqsClient;
    private final QueueInfo queueInfo;
    private final QueueInfo retryQueueInfo;
    private final TaskCallback callback;
    private final SQSWorkerQueueConfiguration queueCfg;
    private final Set<SQSTaskInformation> timeoutSet;
    private final boolean isPoisonMessageConsumer;

    private static final Logger LOG = LoggerFactory.getLogger(SQSMessageConsumer.class);

    public SQSMessageConsumer(
            final SqsClient sqsClient,
            final QueueInfo queueInfo,
            final QueueInfo retryQueueInfo,
            final TaskCallback callback,
            final SQSWorkerQueueConfiguration queueCfg,
            final Set<SQSTaskInformation> timeoutSet,
            final boolean isPoisonMessageConsumer)
    {
        this.sqsClient = sqsClient;
        this.queueInfo = queueInfo;
        this.retryQueueInfo = retryQueueInfo;
        this.callback = callback;
        this.queueCfg = queueCfg;
        this.timeoutSet = timeoutSet;
        this.isPoisonMessageConsumer = isPoisonMessageConsumer;
    }

    @Override
    public void run()
    {
        receiveMessages();
    }

    protected void receiveMessages()
    {
        final var receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueInfo.url())
                .maxNumberOfMessages(queueCfg.getMaxNumberOfMessages())
                .waitTimeSeconds(queueCfg.getLongPollInterval())
                .attributeNamesWithStrings(SQSUtil.ALL_ATTRIBUTES)
                .messageAttributeNames(SQSUtil.ALL_ATTRIBUTES)
                .build();
        while (true) {
            final var receiveMessageResult = sqsClient.receiveMessage(receiveRequest).messages();
            if (receiveMessageResult.isEmpty()) {
                LOG.debug("Nothing received from queue {} ", queueInfo.name());
            }
            for (final var message : receiveMessageResult) {
                LOG.debug("Received {} on queue {} ", message.body(), queueInfo.name());
                registerTask(message);
            }
        }
    }

    private void registerTask(final Message message)
    {
        try {
            final var add = isPoisonMessageConsumer ? queueCfg.getDlqVisibilityTimeout() : queueCfg.getVisibilityTimeout();
            final var becomesVisible = Instant.now().plusSeconds(add);
            final var taskInfo = new SQSTaskInformation(
                    queueInfo,
                    message.messageId(),
                    message.receiptHandle(),
                    becomesVisible,
                    isPoisonMessageConsumer
            );

            final var headers = createHeadersFromMessageAttributes(message);
            callback.registerNewTask(taskInfo, message.body().getBytes(StandardCharsets.UTF_8), headers);
            if (isPoisonMessageConsumer) {
                deleteMessage(message.receiptHandle(),message.messageId());
            } else {
                timeoutSet.add(taskInfo);
            }
        } catch (final TaskRejectedException e) {
            LOG.error("Cannot register new message, rejecting {}", message.messageId(), e);
            retryMessage(message);
        } catch (final InvalidTaskException e) {
            LOG.warn("Message {} rejected as a task at this time, will be redelivered by SQS",
                    message.messageId(), e);
        }
    }

    private void deleteMessage(final String receiptHandle, final String messageId)
    {
        try {
            final var request = DeleteMessageRequest.builder()
                    .queueUrl(queueInfo.url())
                    .receiptHandle(receiptHandle)
                    .build();
            sqsClient.deleteMessage(request);
        } catch (final Exception e) {
            var msg = String.format("Error deleting message from dead letter queue:%s messageId:%s",
                    queueInfo.url(), messageId);
            LOG.error(msg, e);
        }
    }

    // DDD maybe just have a queue for this so we can retry if it fails
    private void retryMessage(final Message message)
    {
        try {
            final var attributes = message.messageAttributes();
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
            var msg = String.format("Error sending message to retry queue:%s messageId:%s",
                    retryQueueInfo.name(), message.messageId());
            LOG.error(msg, e);
        }
    }

    private Map<String, Object> createHeadersFromMessageAttributes(final Message message)
    {
        final var headers = new HashMap<String, Object>();
        for (final Map.Entry<String, MessageAttributeValue> entry: message.messageAttributes().entrySet()) {
            if (entry.getValue().dataType().equals("String")) {
                headers.put(entry.getKey(), entry.getValue().stringValue());
            }
        }
        return headers;
    }
}
