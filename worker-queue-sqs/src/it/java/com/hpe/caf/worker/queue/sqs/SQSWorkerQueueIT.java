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

import org.testng.Assert;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.testng.AssertJUnit.fail;

public class SQSWorkerQueueIT
{
    @Test
    public void testPublish() throws Exception
    {
        var inputQueue = "test-publish";
        var visibilityTimeout = 10;
        var longPollInterval = 1;
        var maxNumberOfMessages = 1;
        var messageRetentionPeriod = 600;
        var maxDeliveries = 1000;
        var workerWrapper = new SQSWorkerQueueWrapper(
                inputQueue,
                visibilityTimeout,
                longPollInterval,
                maxNumberOfMessages,
                maxDeliveries,
                messageRetentionPeriod);
        final var msgBody = "Hello-World";
        sendMessages(workerWrapper, msgBody);

        final var msg = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);
        purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        final var body = msg.body();
        Assert.assertEquals(msgBody, body, "Message was not as expected");
    }

    @Test
    public void testInputQueueIsCreated()
    {
        try {
            var inputQueue = "input-queue-created";
            var visibilityTimeout = 43200;
            var longPollInterval = 10;
            var maxNumberOfMessages = 10;
            var messageRetentionPeriod = 60;
            var maxDeliveries = 1;
            var workerWrapper = new SQSWorkerQueueWrapper(
                    inputQueue,
                    visibilityTimeout,
                    longPollInterval,
                    maxNumberOfMessages,
                    maxDeliveries,
                    messageRetentionPeriod);
            final var getQueueUrlRequest = GetQueueUrlRequest.builder()
                    .queueName(workerWrapper.sqsWorkerQueueConfiguration.getInputQueue())
                    .build();
            workerWrapper.sqsClient.getQueueUrl(getQueueUrlRequest);
        } catch (final Exception e) {
            fail("The input queue was not created:" + e.getMessage());
        }
    }

    /**
     * This test serves to test that the dead letter queue works as expected
     * @throws Exception
     */
    @Test
    public void testRedriveOfMessagesToDeadLetterQueue() throws Exception
    {
        var inputQueue = "test-redrive";
        var visibilityTimeout = 10;
        var longPollInterval = 1;
        var maxNumberOfMessages = 1;
        var messageRetentionPeriod = 600;
        var maxDeliveries = 1;
        var workerWrapper = new SQSWorkerQueueWrapper(
                inputQueue,
                visibilityTimeout,
                longPollInterval,
                maxNumberOfMessages,
                maxDeliveries,
                messageRetentionPeriod);

        final var client = workerWrapper.sqsClient;

        final var msgBody = "Hello-World";
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        sendMessages(client, workerWrapper.inputQueueUrl, messageAttributes, msgBody);

        // Let visibility timeout expire
        Thread.sleep(visibilityTimeout * 1000 + 1000);

        final var msg = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);

        Assert.assertNotNull(msg, "Expected msg");
        purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
    }

    @Test
    public void testSourceQueueIsCopiedFromAttributesToHeadersOnReceipt() throws Exception
    {
        // Not using the input queue so that there is no consumer looping.
        var inputQueue = "test-attributes";
        var visibilityTimeout = 10;
        var longPollInterval = 5;
        var maxNumberOfMessages = 1;
        var messageRetentionPeriod = 600;
        var maxDeliveries = 1;
        var workerWrapper = new SQSWorkerQueueWrapper(
                inputQueue,
                visibilityTimeout,
                longPollInterval,
                maxNumberOfMessages,
                maxDeliveries,
                messageRetentionPeriod);

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

    //@Test  // DDD Retention period is Not implemented in ElasticMQ
    public void testRetentionPeriod() throws Exception
    {
        var inputQueue = "test-retention-period";
        var visibilityTimeout = 10;
        var longPollInterval = 1;
        var maxNumberOfMessages = 1;
        var messageRetentionPeriod = 60;
        var maxDeliveries = 1000;
        var workerWrapper = new SQSWorkerQueueWrapper(
                "worker-in",
                visibilityTimeout,
                longPollInterval,
                maxNumberOfMessages,
                maxDeliveries,
                messageRetentionPeriod);
        final var msgBody = "Hello-World";

        sendMessages(workerWrapper.sqsClient, workerWrapper.inputQueueUrl, msgBody);

        // Let retention period expire
        Thread.sleep(messageRetentionPeriod * 1500);

        final var request = ReceiveMessageRequest.builder()
                .queueUrl(workerWrapper.inputQueueUrl)
                .maxNumberOfMessages(2)
                .waitTimeSeconds(20)
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
        var visibilityTimeout = 600;
        var longPollInterval = 20;
        var maxNumberOfMessages = 10;
        var messageRetentionPeriod = 600;
        var maxDeliveries = 1000;

        var messagesToSend = 3000;
        var workerWrapper = new SQSWorkerQueueWrapper(
                inputQueue,
                visibilityTimeout,
                longPollInterval,
                maxNumberOfMessages,
                maxDeliveries,
                messageRetentionPeriod);
        for (int i = 1; i <= messagesToSend; i++) {
            final var msg = "High Volume Message_" + i;
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
        } while(response != null);

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
        var visibilityTimeout = 10;
        var longPollInterval = 1;
        var maxNumberOfMessages = 1;
        var messageRetentionPeriod = 600;
        var maxDeliveries = 1000;
        var workerWrapper = new SQSWorkerQueueWrapper(
                inputQueue,
                visibilityTimeout,
                longPollInterval,
                maxNumberOfMessages,
                maxDeliveries,
                messageRetentionPeriod);
        final var msgBody = "Redelivery";
        sendMessages(workerWrapper, msgBody);

        final var msg = workerWrapper.callbackQueue.poll(10, TimeUnit.SECONDS);
        final var body = msg.body();
        final var messageId = msg.taskInformation().inboundMessageId();
        Assert.assertEquals(msgBody, body, "Message was not as expected");

        // Let visibility timeout expire
        Thread.sleep(visibilityTimeout * 1000 + 1000);

        final var redeliveredMsg = workerWrapper.callbackQueue.poll(10, TimeUnit.SECONDS);
        purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        final var redeliveredBody = redeliveredMsg.body();
        Assert.assertEquals(
                messageId,
                redeliveredMsg.taskInformation().inboundMessageId(),
                "Message ids do not match");
        Assert.assertEquals(msgBody, redeliveredBody, "Redelivered message was not as expected");
    }

    @Test
    public void testMessageIsNotRedeliveredDuringVisibilityTimeout() throws Exception
    {
        var inputQueue = "during-visibility";
        var visibilityTimeout = 10;
        var longPollInterval = 1;
        var maxNumberOfMessages = 1;
        var messageRetentionPeriod = 600;
        var maxDeliveries = 1000;
        var workerWrapper = new SQSWorkerQueueWrapper(
                inputQueue,
                visibilityTimeout,
                longPollInterval,
                maxNumberOfMessages,
                maxDeliveries,
                messageRetentionPeriod);
        final var msgBody = "No-Redelivery";
        sendMessages(workerWrapper, msgBody);

        final var msg = workerWrapper.callbackQueue.poll(3, TimeUnit.SECONDS);
        final var body = msg.body();
        Assert.assertEquals(msgBody, body,"Message was not as expected");

        var redeliveredMsg = workerWrapper.callbackQueue.poll(3, TimeUnit.SECONDS);
        Assert.assertNull(redeliveredMsg, "Message should not have been redelivered");

        purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
    }

    public static void sendMessages(
            final SQSWorkerQueueWrapper sqsWorkerQueueWrapper,
            final String... messages
    )
    {
        try {
            for (final String message : messages) {
                sendMessage(
                        sqsWorkerQueueWrapper.sqsClient,
                        sqsWorkerQueueWrapper.inputQueueUrl,
                        new HashMap<>(),
                        message
                );
            }
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    public static void sendMessages(
            final SqsClient sqsClient,
            final String queueUrl,
            final String... messages)
    {
        try {
            for (final String message : messages) {
                sendMessage(sqsClient, queueUrl, new HashMap<>(), message);
            }
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    public static void sendMessages(
            final SqsClient sqsClient,
            final String queueUrl,
            final Map<String, MessageAttributeValue> messageAttributes,
            final String... messages)
    {
        try {
            for (final String message : messages) {
                sendMessage(sqsClient, queueUrl, messageAttributes, message);
            }
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    public static void sendMessage(
            final SqsClient sqsClient,
            final String queueUrl,
            final Map<String, MessageAttributeValue> messageAttributes,
            final String message)
    {
        try {
            final var sendRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(message)
                    .messageAttributes(messageAttributes)
                    .build();
            sqsClient.sendMessage(sendRequest);
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    private static void purgeQueue(
            final SqsClient sqsClient,
            final String queueUrl)
    {
        final var purgeQueueRequest = PurgeQueueRequest.builder()
                .queueUrl(queueUrl)
                .build();
        sqsClient.purgeQueue(purgeQueueRequest);

        final var purgeDeadLetterQueueRequest = PurgeQueueRequest.builder()
                .queueUrl(queueUrl + SQSUtil.DEAD_LETTER_QUEUE_SUFFIX)
                .build();
        sqsClient.purgeQueue(purgeDeadLetterQueueRequest);
    }
}
