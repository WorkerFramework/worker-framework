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

import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.worker.queue.sqs.QueueInfo;
import com.hpe.caf.worker.queue.sqs.SQSUtil;
import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

public class QueueConsumer
{
    protected final SqsClient sqsClient;
    protected final QueueInfo queueInfo;
    protected final QueueInfo retryQueueInfo;
    protected final SQSWorkerQueueConfiguration queueCfg;
    protected final TaskCallback callback;


    private static final Logger LOG = LoggerFactory.getLogger(QueueConsumer.class);

    public QueueConsumer(
            final SqsClient sqsClient,
            final QueueInfo queueInfo,
            final QueueInfo retryQueueInfo,
            final SQSWorkerQueueConfiguration queueCfg,
            final TaskCallback callback
    )
    {
        this.sqsClient = sqsClient;
        this.queueInfo = queueInfo;
        this.retryQueueInfo = retryQueueInfo;
        this.queueCfg = queueCfg;
        this.callback = callback;
    }

    protected void retryMessage(final Message message)
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


    protected Map<String, Object> createHeadersFromMessageAttributes(final Message message)
    {
        final var headers = new HashMap<String, Object>();
        for (final Map.Entry<String, MessageAttributeValue> entry : message.messageAttributes().entrySet()) {
            if (entry.getValue().dataType().equals("String")) {
                headers.put(entry.getKey(), entry.getValue().stringValue());
            }
        }
        return headers;
    }
}
