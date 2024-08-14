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

import com.hpe.caf.worker.queue.sqs.distributor.SQSMessageDistributor;
import org.testng.Assert;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
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

import static com.hpe.caf.worker.queue.sqs.SQSWorkerQueueWrapper.getWorkerWrapper;
import static com.hpe.caf.worker.queue.sqs.SQSWorkerQueueWrapper.purgeQueue;
import static com.hpe.caf.worker.queue.sqs.SQSWorkerQueueWrapper.sendMessagesInBatches;
import static com.hpe.caf.worker.queue.sqs.SQSWorkerQueueWrapper.sendMessages;
import static org.testng.AssertJUnit.fail;

public class SQSWorkerQueueIT
{
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
        purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        final var body = msg.body();
        Assert.assertFalse(msg.taskInformation().isPoison());
        Assert.assertEquals(msgBody, body, "Message was not as expected");
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
        purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        Assert.assertTrue(msg.taskInformation().isPoison());
        Assert.assertNotNull(msg, "Expected msg");
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
        Assert.assertTrue(msg.headers().containsKey(SQSUtil.SOURCE_QUEUE),
                "Expected header: " + SQSUtil.SOURCE_QUEUE);
        Assert.assertEquals(msg.headers().get(SQSUtil.SOURCE_QUEUE).toString(), inputQueue, "Expected:" + inputQueue);
        purgeQueue(workerWrapper.sqsClient, queueUrl);
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

        purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);

        Assert.assertEquals(response.messages().size(), 0,
                "Message should have been discarded after retention expired.");
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

        purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);

        var messages = receiveMessageResult.stream().map(m -> m.body()).collect(Collectors.toList());
        var messagesSet = messages.stream().collect(Collectors.toSet());
        Assert.assertTrue(messages.size() == messagesToSend, "Count of messages received was not as expected");
        Assert.assertTrue(messagesSet.size() == messagesToSend, "Duplicate messages detected");
    }

    @Test
    public void testMessageIsRedeliveredWithSameMessageIdAfterVisibilityTimeoutExpires() throws Exception
    {
        var inputQueue = "expired-visibility";
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                10,
                1,
                1,
                1000,
                600);
        final var msgBody = "Redelivery";
        sendMessages(workerWrapper, msgBody);

        final var msg = workerWrapper.callbackQueue.poll(10, TimeUnit.SECONDS);
        final var body = msg.body();
        final var messageId = msg.taskInformation().getInboundMessageId();
        Assert.assertEquals(msgBody, body, "Message was not as expected");

        // Let visibility timeout expire
        Thread.sleep(10 * 1000 + 1000);

        final var redeliveredMsg = workerWrapper.callbackQueue.poll(10, TimeUnit.SECONDS);
        purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        final var redeliveredBody = redeliveredMsg.body();
        Assert.assertEquals(
                messageId,
                redeliveredMsg.taskInformation().getInboundMessageId(),
                "Message ids do not match");
        Assert.assertEquals(msgBody, redeliveredBody, "Redelivered message was not as expected");
    }

    @Test
    public void testMessageIsNotRedeliveredDuringVisibilityTimeout() throws Exception
    {
        var inputQueue = "during-visibility";
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                10,
                1,
                1,
                1000,
                600);
        final var msgBody = "No-Redelivery";
        sendMessages(workerWrapper, msgBody);

        final var msg = workerWrapper.callbackQueue.poll(3, TimeUnit.SECONDS);
        final var body = msg.body();
        Assert.assertEquals(msgBody, body, "Message was not as expected");

        var redeliveredMsg = workerWrapper.callbackQueue.poll(3, TimeUnit.SECONDS);
        Assert.assertNull(redeliveredMsg, "Message should not have been redelivered");

        purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
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

        Assert.assertEquals(10, receivedMessages.size(), "Message batch was not as expected");

        purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
    }

    @Test
    public void testRedistributeMessagesWhenNoneExistDoesNotThrowError() throws Exception
    {
        var destinationQueue = "test-redistribute-none-destination";
        var sourceQueue = "test-redistribute-none-source";
        var numberOfMessages = 0;
        var numberOfMessagesToMove = 15;
        var expectedNumberOfMessagesToMove = 0;
        runRedistributionTest(
                sourceQueue,
                destinationQueue,
                numberOfMessages,
                numberOfMessagesToMove,
                expectedNumberOfMessagesToMove
        );
    }

    @Test
    public void testRedistributeMessagesInSingleBatch() throws Exception
    {
        var destinationQueue = "test-redistribute-destination";
        var sourceQueue = "test-redistribute-source";
        var numberOfMessages = 100;
        var numberOfMessagesToMove = 15;
        var expectedNumberOfMessagesToMove = 10;
        runRedistributionTest(
                sourceQueue,
                destinationQueue,
                numberOfMessages,
                numberOfMessagesToMove,
                expectedNumberOfMessagesToMove
        );
    }

    @Test
    public void testRedistributeMessagesInMultipleBatches() throws Exception
    {
        var destinationQueue = "test-redistribute-multi-destination";
        var sourceQueue = "test-redistribute-multi-source";
        var numberOfMessages = 100;
        var numberOfMessagesToMove = 100;
        var expectedNumberOfMessagesToMove = 100;
        runRedistributionTest(
                sourceQueue,
                destinationQueue,
                numberOfMessages,
                numberOfMessagesToMove,
                expectedNumberOfMessagesToMove
        );
    }

    private void runRedistributionTest(
        final String sourceQueue,
        final String destinationQueue,
        final int numberOfMessages,
        final int numberOfMessagesToMove,
        final int expectedMessagesToBeMoved
    ) throws Exception
    {
        final var workerWrapper = getWorkerWrapper(
                destinationQueue,
                60,
                1,
                10,
                1,
                600);

        var createQueueRequest = CreateQueueRequest.builder()
                .queueName(sourceQueue)
                .build();
        var sourceQueueUrl = workerWrapper.sqsClient.createQueue(createQueueRequest).queueUrl();

        sendMessagesInBatches(workerWrapper.sqsClient, sourceQueueUrl, numberOfMessages);

        var distributor = new SQSMessageDistributor(
                new SQSClientProviderImpl(workerWrapper.sqsConfiguration),
                sourceQueue,
                destinationQueue
        );

        var moveMessagesResult = distributor.moveMessages(numberOfMessagesToMove);

        var receivedMessages = new ArrayList<CallbackResponse>();
        CallbackResponse response;
        do {
            response = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
            if (response != null) {
                receivedMessages.add(response);
            }
        } while (response != null);

        Assert.assertEquals(receivedMessages.size(), expectedMessagesToBeMoved, "Message batch was not as expected");
        Assert.assertEquals(moveMessagesResult.size(), expectedMessagesToBeMoved, "Move message result was not as expected");

        purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        purgeQueue(workerWrapper.sqsClient, sourceQueueUrl);
    }
}
