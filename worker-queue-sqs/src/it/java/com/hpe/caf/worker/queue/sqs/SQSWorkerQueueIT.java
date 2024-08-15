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
import org.testng.Assert;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.deleteMessage;
import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.getWorkerWrapper;
import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.purgeQueue;
import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.receiveMessages;
import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.sendMessages;
import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.sendMessagesInBatches;
import static org.testng.AssertJUnit.fail;

public class SQSWorkerQueueIT
{
     // DDD
//    [ERROR] Failures:
//        [ERROR]   SQSWorkerQueueIT.testDeleteFailsWhenReceiptHandleHasExpired:313 Should have got one message expected [0] but found [1]
//        [ERROR]   SQSWorkerQueueIT.testRedriveOfMessagesToDeadLetterQueue:115 NullPointer Cannot invoke "com.hpe.caf.worker.queue.sqs.util.CallbackResponse.taskInformation()" because "msg" is null



    @Test
    public void testPublish() throws Exception
    {
        var inputQueue = "test-publish";
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
            Assert.assertFalse(msg.taskInformation().isPoison());
            Assert.assertEquals(msgBody, body, "Message was not as expected");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testInputQueueIsCreated()
    {
        try {
            var inputQueue = "input-queue-created";
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
    public void testRedriveOfMessagesToDeadLetterQueue() throws Exception
    {
        var inputQueue = "test-redrive";
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                10,
                1,
                1,
                1,
                600);

        final var client = workerWrapper.sqsClient;

        final var msgBody = "Hello-World";
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        sendMessages(client, workerWrapper.inputQueueUrl, messageAttributes, msgBody);

        // Let visibility timeout expire
        Thread.sleep(10 * 1000 + 1000);

        final var msg = workerWrapper.callbackDLQ.poll(30, TimeUnit.SECONDS);

        try {
            Assert.assertTrue(msg.taskInformation().isPoison());
            Assert.assertNotNull(msg, "Expected msg");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testAllAttributesAreCopiedToHeadersOnReceipt() throws Exception
    {
        var inputQueue = "test-attributes";
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                10,
                5,
                1,
                1,
                600);

        final var client = workerWrapper.sqsClient;

        final var msgBody = "Hello-World";
        var queueUrl = SQSUtil.getQueueUrl(workerWrapper.sqsClient, inputQueue);
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put(SQSUtil.SOURCE_QUEUE, MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(inputQueue)
                .build());

        sendMessages(client, queueUrl, messageAttributes, msgBody);

        final var msg = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);
        try {
            Assert.assertTrue(msg.headers().containsKey(SQSUtil.SOURCE_QUEUE),
                    "Expected header: " + SQSUtil.SOURCE_QUEUE);
            Assert.assertEquals(msg.headers().get(SQSUtil.SOURCE_QUEUE).toString(), inputQueue, "Expected:" + inputQueue);
        } finally {
            purgeQueue(workerWrapper.sqsClient, queueUrl);
        }
    }

    @Test
    public void testRetentionPeriod() throws Exception
    {
        var inputQueue = "test-retention-period";
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                10,
                1,
                1,
                1000,
                60);
        final var msgBody = "Hello-World";

        var attributesRequest = GetQueueAttributesRequest.builder()
                .queueUrl(workerWrapper.inputQueueUrl)
                .attributeNames(QueueAttributeName.MESSAGE_RETENTION_PERIOD)
                .build();
        var attributesResponse = workerWrapper.sqsClient.getQueueAttributes(attributesRequest);

        try {
            Assert.assertTrue(
                    attributesResponse.attributes().containsKey(QueueAttributeName.MESSAGE_RETENTION_PERIOD),
                    "Expected attribute: " + QueueAttributeName.MESSAGE_RETENTION_PERIOD);
            Assert.assertEquals(
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
            var response = workerWrapper.sqsClient.receiveMessage(request);

            Assert.assertEquals(response.messages().size(), 0,
                    "Message should have been discarded after retention expired.");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testHighVolumeOfMessagesDoesNotContainDuplicates() throws Exception
    {
        var inputQueue = "high-volume-worker-in";
        var messagesToSend = 3000;
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

        var receiveMessageResult = new ArrayList<CallbackResponse>();
        CallbackResponse response;
        do {
            // This is polling the internal BlockingQueue created in our test callback
            response = workerWrapper.callbackQueue.poll(10, TimeUnit.SECONDS);
            if (response != null) {
                receiveMessageResult.add(response);
            }
        } while (response != null);

        var messages = receiveMessageResult.stream().map(m -> m.body()).collect(Collectors.toList());
        var messagesSet = messages.stream().collect(Collectors.toSet());
        try {
            Assert.assertTrue(messages.size() == messagesToSend, "Count of messages received was not as expected");
            Assert.assertTrue(messagesSet.size() == messagesToSend, "Duplicate messages detected");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testMessageBatchesAreDelivered() throws Exception
    {
        var inputQueue = "test-batch";
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                60,
                1,
                10,
                1,
                600);
        var messagesToSend = 10;
        sendMessagesInBatches(workerWrapper.sqsClient, workerWrapper.inputQueueUrl, messagesToSend);

        var receivedMessages = new ArrayList<CallbackResponse>();
        CallbackResponse response;
        do {
            // This is polling the internal BlockingQueue created in our test callback
            response = workerWrapper.callbackQueue.poll(10, TimeUnit.SECONDS);
            if (response != null) {
                receivedMessages.add(response);
            }
        } while (response != null);

        try {
            Assert.assertEquals(10, receivedMessages.size(), "Message batch was not as expected");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testDeleteFailsWhenReceiptHandleHasExpired() throws Exception
    {

        var testQueue = "expired-receipt-handle";
        final var workerWrapper = getWorkerWrapper(
                "unused-expired-receipt-handle",
                10,
                10,
                1,
                1000,
                600);
        final var msgBody = "hello-world";

        try {
            var createQueueRequest = CreateQueueRequest.builder()
                    .queueName(testQueue)
                    .build();
            var testQueueUrl = workerWrapper.sqsClient.createQueue(createQueueRequest).queueUrl();
            var queueInfo = SQSUtil.getQueueInfo(workerWrapper.sqsClient, testQueue);

            sendMessages(workerWrapper.sqsClient, testQueueUrl, new HashMap<>(), msgBody);

            var messages = receiveMessages(workerWrapper.sqsClient, testQueueUrl, 5).messages();
            Assert.assertEquals(1, messages.size(), "Should have got one message");
            var msg = messages.get(0);

            // This task info will eventually have an invalid receipt handle
            // Once the message is redelivered.
            final var sqsTaskInfo = new SQSTaskInformation(
                    queueInfo,
                    msg.messageId(),
                    msg.receiptHandle(),
                    Instant.now().plusSeconds(60),
                    false
            );

            // Allow timeout to expire
            Thread.sleep(10000);
            messages = receiveMessages(workerWrapper.sqsClient, testQueueUrl, 10).messages();
            Assert.assertEquals(1, messages.size(), "Should have got one message");

            // Now try to delete using an expired receipt handle, request will not fail
            // but message should not be deleted.
            deleteMessage(workerWrapper.sqsClient, sqsTaskInfo);

            Thread.sleep(2000);
            messages = receiveMessages(workerWrapper.sqsClient, testQueueUrl, 10).messages();
            Assert.assertEquals(1, messages.size(), "Should have got one message");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }
}
