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
import com.hpe.caf.worker.queue.sqs.SQSTaskInformation;
import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;
import com.hpe.caf.worker.queue.sqs.util.SQSMetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;

public class DeadLetterQueueConsumer extends QueueConsumer
{
    private static final Logger LOG = LoggerFactory.getLogger(DeadLetterQueueConsumer.class);

    public DeadLetterQueueConsumer(
            final SqsClient sqsClient,
            final QueueInfo queueInfo,
            final QueueInfo retryQueueInfo,
            final TaskCallback callback,
            final SQSWorkerQueueConfiguration queueCfg, SQSMetricsReporter sqsMetricsReporter)
    {
        super(sqsClient, queueInfo, retryQueueInfo, queueCfg, callback, sqsMetricsReporter);
    }

    @Override
    protected void handleRegistrationTasks(final SQSTaskInformation taskInfo)
    {
        deleteMessage(taskInfo.getReceiptHandle(), taskInfo.getInboundMessageId());
    }

    private void deleteMessage(final String receiptHandle, final String messageId)
    {
        try {
            final var request = DeleteMessageRequest.builder()
                    .queueUrl(queueInfo.url())
                    .receiptHandle(receiptHandle)
                    .build();
            sqsClient.deleteMessage(request);
            sqsMetricsReporter.incrementDropped();
        } catch (final Exception e) {
            var msg = String.format("Error deleting message from dead letter queue:%s messageId:%s",
                    queueInfo.url(), messageId);
            LOG.error(msg, e);
            sqsMetricsReporter.incrementErrors();
        }
    }

    @Override
    protected int getVisibilityTimeout()
    {
        return queueCfg.getDlqVisibilityTimeout();
    }

    @Override
    protected boolean isPoisonMessageConsumer()
    {
        return true;
    }
}
