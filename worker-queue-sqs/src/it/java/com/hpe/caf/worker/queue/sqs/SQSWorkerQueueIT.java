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
import com.hpe.caf.worker.queue.sqs.util.WrapperConfig;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.getWorkerWrapper;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.purgeQueue;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.sendMessages;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.sendMessagesInBatches;
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
        sendMessages(workerWrapper, msgBody);
        try {
            final var msg = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);
            final var body = msg.body();
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
                    new WrapperConfig());
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

    @Test
    public void testRetentionPeriod() throws Exception
    {
        final var inputQueue = "test-retention-period";
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                new WrapperConfig(
                    10,
                    5,
                    1,
                    1000,
                    60
                )
        );
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
                    "60",
                    attributesResponse.attributes().get(QueueAttributeName.MESSAGE_RETENTION_PERIOD),
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
        final var workerWrapper = getWorkerWrapper(inputQueue);

        for(int i = 1; i <= messagesToSend; i++) {
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

        final var messages = receiveMessageResult.stream().map(CallbackResponse::body).toList();
        final var messagesSet = new HashSet<>(messages);
        try {
            assertEquals(messages.size(), messagesToSend, "Count of messages received was not as expected");
            assertEquals(messagesSet.size(), messagesToSend, "Duplicate messages detected");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testMessageBatchesAreDelivered() throws Exception
    {
        final var inputQueue = "test-batch";
        final var workerWrapper = getWorkerWrapper(inputQueue);
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
