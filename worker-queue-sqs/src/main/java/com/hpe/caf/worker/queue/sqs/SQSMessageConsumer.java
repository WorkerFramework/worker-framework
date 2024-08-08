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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SQSMessageConsumer implements Runnable
{
    private final SqsClient sqsClient;
    private final QueueInfo queueInfo;
    private final TaskCallback callback;
    private final SQSWorkerQueueConfiguration sqsQueueConfiguration;
    private final boolean isPoisonMessageConsumer;

    private static final Logger LOG = LoggerFactory.getLogger(SQSMessageConsumer.class);

    public SQSMessageConsumer(
            final SqsClient sqsClient,
            final QueueInfo queueInfo,
            final TaskCallback callback,
            final SQSWorkerQueueConfiguration sqsQueueConfiguration,
            final boolean isPoisonMessageConsumer)
    {
        this.sqsClient = sqsClient;
        this.queueInfo = queueInfo;
        this.callback = callback;
        this.sqsQueueConfiguration = sqsQueueConfiguration;
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
                .maxNumberOfMessages(sqsQueueConfiguration.getMaxNumberOfMessages())
                .waitTimeSeconds(sqsQueueConfiguration.getLongPollInterval())
                .attributeNamesWithStrings(SQSUtil.ALL_ATTRIBUTES)
                .messageAttributeNames(SQSUtil.ALL_ATTRIBUTES)
                .build();
        while (true) {
            final var receiveMessageResult = sqsClient.receiveMessage(receiveRequest).messages();
            for (final var message : receiveMessageResult) {
                LOG.debug("Received {} on queue {} ", message.body(), queueInfo.url());
                registerTask(message);
            }
        }
    }

    private void registerTask(final Message message)
    {
        final var taskInfo = new SQSTaskInformation(
                queueInfo,
                message.messageId(),
                message.receiptHandle(),
                isPoisonMessageConsumer
        );
        try {
            final var headers = createHeadersFromMessageAttributes(message);
            callback.registerNewTask(taskInfo, message.body().getBytes(StandardCharsets.UTF_8), headers);
            if (isPoisonMessageConsumer) {
                deleteMessage(message.receiptHandle(),message.messageId());
            }
        } catch (final TaskRejectedException e) {
            throw new RuntimeException("Task rejected", e); // DDD what here
        } catch (final InvalidTaskException e) {
            throw new RuntimeException("Invalid task", e); // DDD What here
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
