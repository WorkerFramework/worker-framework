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

import com.hpe.caf.worker.queue.sqs.util.SQSUtil;
import com.hpe.caf.worker.queue.sqs.util.WrapperConfig;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.getWorkerWrapper;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.purgeQueue;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.sendMessages;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SQSWorkerQueueIT
{
    @Test
    public void testPublish() throws Exception
    {
        final var inputQueue = "test-publish";
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                new WrapperConfig());
        final var msgBody = "Hello-World";
        final var metricsReporter = workerWrapper.metricsReporter;
        sendMessages(workerWrapper, msgBody);
        try {
            final var msg = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);
            final var body = msg.body();
            assertFalse(msg.taskInformation().isPoison());
            assertEquals(msgBody, body, "Message was not as expected");
            assertEquals(1, metricsReporter.getMessagesReceived(),
                    "Metrics should only have reported a single message");
            assertEquals(0, metricsReporter.getQueueErrors(),
                    "Metrics should not have reported errors");
            assertEquals(0, metricsReporter.getMessagesDropped(),
                    "Metrics should not have reported dropped messages");
            assertEquals(0, metricsReporter.getMessagesRejected(),
                    "Metrics should not have reported rejected messages");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testInputQueueIsCreated()
    {
        try {
            final var inputQueue = "input-queue-created";
            final var workerWrapper = getWorkerWrapper(
                    inputQueue,
                    new WrapperConfig());
            final var getQueueUrlRequest = GetQueueUrlRequest.builder()
                    .queueName(workerWrapper.workerQueueConfiguration.getInputQueue())
                    .build();
            workerWrapper.sqsClient.getQueueUrl(getQueueUrlRequest);
        } catch (final Exception e) {
            fail("The input queue was not created:" + e.getMessage());
        }
    }

    @Test
    public void testAllAttributesAreCopiedToHeadersOnReceipt() throws Exception
    {
        final var inputQueue = "test-attributes";
        final var workerWrapper = getWorkerWrapper(inputQueue);

        final var client = workerWrapper.sqsClient;

        final var msgBody = "Hello-World";
        final var queueUrl = SQSUtil.getQueueUrl(workerWrapper.sqsClient, inputQueue);
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put(SQSUtil.SOURCE_QUEUE, MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(inputQueue)
                .build());

        sendMessages(client, queueUrl, messageAttributes, msgBody);

        try {
            final var msg = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);
            assertTrue(msg.headers().containsKey(SQSUtil.SOURCE_QUEUE),
                    "Expected header: " + SQSUtil.SOURCE_QUEUE);
            assertEquals(msg.headers().get(SQSUtil.SOURCE_QUEUE).toString(), inputQueue, "Expected:" + inputQueue);
        } finally {
            purgeQueue(workerWrapper.sqsClient, queueUrl);
        }
    }
}
