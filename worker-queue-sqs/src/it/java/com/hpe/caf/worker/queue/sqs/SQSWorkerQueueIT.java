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

import com.hpe.caf.worker.queue.sqs.util.CallbackResponse;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.getWorkerWrapper;
import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.purgeQueue;
import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.sendMessages;
import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.sendMessagesInBatches;
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
                10,
                1,
                1,
                1000,
                600);
        final var msgBody = "Hello-World";
        sendMessages(workerWrapper, msgBody);

        final var msg = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);

        final var body = msg.body();
        try {
            assertFalse(msg.taskInformation().isPoison());
            assertEquals(msgBody, body, "Message was not as expected");
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
                    10,
                    1,
                    1,
                    1,
                    600);
            final var getQueueUrlRequest = GetQueueUrlRequest.builder()
                    .queueName(workerWrapper.sqsWorkerQueueConfiguration.getInputQueue())
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
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                10,
                5,
                1,
                1,
                600);

        final var client = workerWrapper.sqsClient;

        final var msgBody = "Hello-World";
        final var queueUrl = SQSUtil.getQueueUrl(workerWrapper.sqsClient, inputQueue);
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put(SQSUtil.SOURCE_QUEUE, MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(inputQueue)
                .build());

        sendMessages(client, queueUrl, messageAttributes, msgBody);

        final var msg = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);
        try {
            assertTrue(msg.headers().containsKey(SQSUtil.SOURCE_QUEUE),
                    "Expected header: " + SQSUtil.SOURCE_QUEUE);
            assertEquals(msg.headers().get(SQSUtil.SOURCE_QUEUE).toString(), inputQueue, "Expected:" + inputQueue);
        } finally {
            purgeQueue(workerWrapper.sqsClient, queueUrl);
        }
    }

    @Test
    public void testRetentionPeriod() throws Exception
    {
        final var inputQueue = "test-retention-period";
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                10,
                1,
                1,
                1000,
                60);
        final var msgBody = "Hello-World";

        final var attributesRequest = GetQueueAttributesRequest.builder()
                .queueUrl(workerWrapper.inputQueueUrl)
                .attributeNames(QueueAttributeName.MESSAGE_RETENTION_PERIOD)
                .build();
        final var attributesResponse = workerWrapper.sqsClient.getQueueAttributes(attributesRequest);

        try {
            assertTrue(
                    attributesResponse.attributes().containsKey(QueueAttributeName.MESSAGE_RETENTION_PERIOD),
                    "Expected attribute: " + QueueAttributeName.MESSAGE_RETENTION_PERIOD);
            assertEquals(
                    attributesResponse.attributes().get(QueueAttributeName.MESSAGE_RETENTION_PERIOD),
                    "60",
                    "Message retention period was not as expected");

            sendMessages(workerWrapper, msgBody);

            // Let retention period expire
            Thread.sleep(60 * 1500);

            final var request = ReceiveMessageRequest.builder()
                    .queueUrl(workerWrapper.inputQueueUrl)
                    .maxNumberOfMessages(1)
                    .messageAttributeNames(SQSUtil.ALL_ATTRIBUTES)
                    .attributeNamesWithStrings(SQSUtil.ALL_ATTRIBUTES)
                    .build();
            final var response = workerWrapper.sqsClient.receiveMessage(request);

            assertEquals(response.messages().size(), 0,
                    "Message should have been discarded after retention expired.");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testHighVolumeOfMessagesDoesNotContainDuplicates() throws Exception
    {
        final var inputQueue = "high-volume-worker-in";
        final var messagesToSend = 3000;
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                600,
                20,
                10,
                1000,
                600);

        for (int i = 1; i <= messagesToSend; i++) {
            final var msg = String.format("High Volume Message:%d", i);
            sendMessages(workerWrapper, msg);
        }

        final var receiveMessageResult = new ArrayList<CallbackResponse>();
        CallbackResponse response;
        do {
            // This is polling the internal BlockingQueue created in our test callback
            response = workerWrapper.callbackQueue.poll(10, TimeUnit.SECONDS);
            if (response != null) {
                receiveMessageResult.add(response);
            }
        } while (response != null);

        final var messages = receiveMessageResult.stream().map(m -> m.body()).collect(Collectors.toList());
        final var messagesSet = messages.stream().collect(Collectors.toSet());
        try {
            assertTrue(messages.size() == messagesToSend, "Count of messages received was not as expected");
            assertTrue(messagesSet.size() == messagesToSend, "Duplicate messages detected");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testMessageBatchesAreDelivered() throws Exception
    {
        final var inputQueue = "test-batch";
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                60,
                1,
                10,
                1,
                600);
        final var messagesToSend = 10;
        sendMessagesInBatches(workerWrapper.sqsClient, workerWrapper.inputQueueUrl, messagesToSend);

        final var receivedMessages = new ArrayList<CallbackResponse>();
        CallbackResponse response;
        do {
            // This is polling the internal BlockingQueue created in our test callback
            response = workerWrapper.callbackQueue.poll(10, TimeUnit.SECONDS);
            if (response != null) {
                receivedMessages.add(response);
            }
        } while (response != null);

        try {
            assertEquals(10, receivedMessages.size(), "Message batch was not as expected");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }
}
