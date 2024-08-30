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
import com.hpe.caf.worker.queue.sqs.config.WorkerQueueConfiguration;
import com.hpe.caf.worker.queue.sqs.metrics.MetricsReporter;
import com.hpe.caf.worker.queue.sqs.publisher.PublishEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiptHandleIsInvalidException;

import java.util.concurrent.BlockingQueue;

public class DeadLetterQueueConsumer extends QueueConsumer
{
    private static final Logger LOG = LoggerFactory.getLogger(DeadLetterQueueConsumer.class);

    public DeadLetterQueueConsumer(
            final SqsClient sqsClient,
            final QueueInfo queueInfo,
            final QueueInfo retryQueueInfo,
            final TaskCallback callback,
            final WorkerQueueConfiguration queueCfg,
            final MetricsReporter metricsReporter,
            final BlockingQueue<PublishEvent> publisherQueue)
    {
        super(sqsClient, queueInfo, retryQueueInfo, queueCfg, callback, metricsReporter, publisherQueue);
    }

    @Override
    protected void handleRegistrationTasks(final SQSTaskInformation taskInfo)
    {
        deleteMessage(taskInfo);
    }

    private void deleteMessage(final SQSTaskInformation taskInfo)
    {
        try {
            final var request = DeleteMessageRequest.builder()
                    .queueUrl(queueInfo.url())
                    .receiptHandle(taskInfo.getReceiptHandle())
                    .build();
            publisherQueue.add(new PublishEvent(request));
            metricsReporter.incrementDropped();
        } catch (final ReceiptHandleIsInvalidException e) {
            LOG.error("Receipt handle: {} is invalid", taskInfo, e);
            metricsReporter.incrementErrors();
        } catch (final QueueDoesNotExistException e) {
            LOG.error("Queue may have been deleted {}", taskInfo, e);
            metricsReporter.incrementErrors();
        } catch (final Exception e) {
            var msg = String.format("Error deleting message from dead letter queue:%s messageId:%s",
                    queueInfo.url(), taskInfo.getInboundMessageId());
            LOG.error(msg, e);
            metricsReporter.incrementErrors();
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
