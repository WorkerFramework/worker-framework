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

import com.hpe.caf.api.worker.TaskInformation;
import org.testng.Assert;
import org.testng.annotations.Test;

import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.testng.AssertJUnit.fail;

public class SQSWorkerQueueIT
{
    @Test
    public void testInputQueueIsCreated()
    {
        try {
            var inputQueue = "input-queue-created";
            var workerWrapper = new SQSWorkerQueueWrapper(inputQueue);
            final var getQueueUrlRequest = GetQueueUrlRequest.builder()
                    .queueName(workerWrapper.sqsWorkerQueueConfiguration.getInputQueue())
                    .build();
            workerWrapper.sqsClient.getQueueUrl(getQueueUrlRequest);
        } catch (final Exception e) {
            fail("The input queue was not created:" + e.getMessage());
        }
    }

    @Test
    public void testPublish() throws Exception
    {
        var inputQueue = "test-publish";
        var workerWrapper = new SQSWorkerQueueWrapper(inputQueue);
        final var msgBody = "Hello-World";
        sendMessage(workerWrapper, msgBody);

        final var msg = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);
        deleteMessage(workerWrapper, getReceiptHandle(msg.taskInformation()));
        final var body = msg.body();
        Assert.assertEquals(body, msgBody, "Message was not as expected");
    }

    @Test
    public void testHighVolumeOfMessagesDoesNotContainDuplicates() throws Exception
    {
        var inputQueue = "high-volume-worker-in";
        var visibilityTimeout = 43200;
        var longPollInterval = 10;
        var maxNumberOfMessages = 10;

        var messagesToSend = 3000;
        var workerWrapper = new SQSWorkerQueueWrapper(inputQueue, visibilityTimeout, longPollInterval, maxNumberOfMessages);
        for (int i = 1; i <= messagesToSend; i++) {
            final var msg = "High Volume Message_" + i;
            sendMessage(workerWrapper, msg);
        }

        var receiveMessageResult = new ArrayList<CallbackResponse>();
        CallbackResponse response;
        do {
            // This is polling the internal BlockingQueue created in our test callback
            response = workerWrapper.callbackQueue.poll(10, TimeUnit.SECONDS);
            if (response != null) {
                receiveMessageResult.add(response);
                deleteMessage(workerWrapper, getReceiptHandle(response.taskInformation()));
            }
        } while(response != null);

        var messages = receiveMessageResult.stream().map(m -> m.body()).collect(Collectors.toList());
        var messagesSet = messages.stream().collect(Collectors.toSet());
        Assert.assertTrue(messages.size() == messagesToSend, "Count of messages received was not as expected");
        Assert.assertTrue(messagesSet.size() == messagesToSend, "Duplicate messages detected");
    }

    @Test
    public void testMessageIsRedeliveredAfterVisibilityTimeoutExpires() throws Exception
    {
        var inputQueue = "expired-visibility";
        var workerWrapper = new SQSWorkerQueueWrapper(inputQueue);
        final var msgBody = "Redelivery";
        sendMessage(workerWrapper, msgBody);

        final var msg = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);
        final var body = msg.body();
        final var messageId = msg.taskInformation().getInboundMessageId();
        Assert.assertEquals(body, msgBody, "Message was not as expected");

        final var redeliveredMsg = workerWrapper.callbackQueue.poll(35, TimeUnit.SECONDS);
        deleteMessage(workerWrapper, getReceiptHandle(redeliveredMsg.taskInformation()));
        final var redeliveredBody = redeliveredMsg.body();
        Assert.assertEquals(
                messageId,
                redeliveredMsg.taskInformation().getInboundMessageId(),
                "Message isd do not match");
        Assert.assertEquals(body, redeliveredBody, "Redelivered message was not as expected");
    }

    @Test
    public void testMessageIsNotRedeliveredDuringVisibilityTimeout() throws Exception
    {
        var inputQueue = "during-visibility";
        var workerWrapper = new SQSWorkerQueueWrapper(inputQueue);
        final var msgBody = "No-Redelivery";
        sendMessage(workerWrapper, msgBody);

        final var msg = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);
        final var body = msg.body();
        Assert.assertEquals(body, msgBody, "Message was not as expected");

        var redeliveredMsg = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
        Assert.assertNull(redeliveredMsg, "Message should not have been redelivered");

        //  Now get the message and delete
        redeliveredMsg = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);
        deleteMessage(workerWrapper, getReceiptHandle(redeliveredMsg.taskInformation()));
    }

    public static void sendMessage(final SQSWorkerQueueWrapper sqsQueue, final String... messages)
    {
        try {
            for (final String message : messages) {
                final var sendRequest = SendMessageRequest.builder()
                        .queueUrl(sqsQueue.inputQueueUrl)
                        .messageBody(message)
                        .build();
                sqsQueue.sqsClient.sendMessage(sendRequest);
            }
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    private static void deleteMessage(final SQSWorkerQueueWrapper sqsQueue, final String receiptHandle)
    {
        final var deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(sqsQueue.inputQueueUrl)
                .receiptHandle(receiptHandle)
                .build();
        sqsQueue.sqsClient.deleteMessage(deleteRequest);
    }

    private static String getReceiptHandle(final TaskInformation taskInformation)
    {
        var sqsTaskInformation = (SQSTaskInformation)taskInformation;
        return sqsTaskInformation.getReceiptHandle();
    }
}
