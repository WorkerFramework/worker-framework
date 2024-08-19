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
import com.hpe.caf.worker.queue.sqs.SQSUtil;
import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;
import com.hpe.caf.worker.queue.sqs.visibility.VisibilityMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class InputQueueConsumer extends QueueConsumer implements Runnable
{

    private final VisibilityMonitor visibilityMonitor;

    private static final Logger LOG = LoggerFactory.getLogger(InputQueueConsumer.class);

    public InputQueueConsumer(
            final SqsClient sqsClient,
            final QueueInfo queueInfo,
            final QueueInfo retryQueueInfo,
            final TaskCallback callback,
            final SQSWorkerQueueConfiguration queueCfg,
            final VisibilityMonitor visibilityMonitor)
    {
        super(sqsClient, queueInfo, retryQueueInfo, queueCfg, callback);
        this.visibilityMonitor = visibilityMonitor;
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
                .attributeNamesWithStrings(SQSUtil.ALL_ATTRIBUTES) // DDD may not require this
                .messageAttributeNames(SQSUtil.ALL_ATTRIBUTES) // DDD may not require this
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
            final var becomesVisible = Instant.now().plusSeconds(queueCfg.getVisibilityTimeout());
            final var taskInfo = new SQSTaskInformation(
                    queueInfo,
                    message.messageId(),
                    message.receiptHandle(),
                    becomesVisible,
                    false
            );

            final var headers = createHeadersFromMessageAttributes(message);
            callback.registerNewTask(taskInfo, message.body().getBytes(StandardCharsets.UTF_8), headers);
            visibilityMonitor.watch(taskInfo);
        } catch (final TaskRejectedException e) {
            LOG.error("Cannot register new message, rejecting {}", message.messageId(), e);
            retryMessage(message);
        } catch (final InvalidTaskException e) {
            LOG.warn("Message {} rejected as a task at this time, will be redelivered by SQS",
                    message.messageId(), e);
        }
    }
}
