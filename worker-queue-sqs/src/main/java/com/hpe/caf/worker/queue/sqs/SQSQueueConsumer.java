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
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.nio.charset.StandardCharsets;

public class SQSQueueConsumer implements Runnable
{
    private final SqsClient sqsClient;
    private final String queueUrl;
    private final TaskCallback callback;
    private final SQSWorkerQueueConfiguration sqsQueueConfiguration;

    private static final Logger LOG = LoggerFactory.getLogger(SQSQueueConsumer.class);

    public SQSQueueConsumer(
            final SqsClient sqsClient,
            final String queueUrl,
            final TaskCallback callback, SQSWorkerQueueConfiguration sqsQueueConfiguration)
    {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.callback = callback;
        this.sqsQueueConfiguration = sqsQueueConfiguration;
    }

    @Override
    public void run()
    {
        receiveMessages();
    }

    public void receiveMessages()
    {
        while (true) {
            final var receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(1) // DDD configurable??
                    .waitTimeSeconds(sqsQueueConfiguration.getLongPollInterval())
                    .build();
            final var receiveMessageResult = sqsClient.receiveMessage(receiveRequest).messages();
            for (final var message : receiveMessageResult) {
                LOG.debug("Received {} on queue {} ", message.body(), queueUrl);
                registerTask(message);
            }
        }
    }

    private void registerTask(final Message message)
    {
        final var taskInfo = new SQSTaskInformation(message.messageId(), message.receiptHandle(), false);
        try {
            callback.registerNewTask(taskInfo, message.body().getBytes(StandardCharsets.UTF_8), null);
        } catch (final TaskRejectedException e) {
            throw new RuntimeException("Task rejected", e); // DDD what here
        } catch (final InvalidTaskException e) {
            throw new RuntimeException("Invalid task", e); // DDD What here
        }
    }
}
