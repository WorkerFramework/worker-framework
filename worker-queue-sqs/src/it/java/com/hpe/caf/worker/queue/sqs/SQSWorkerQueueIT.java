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

import com.hpe.caf.configs.SQSConfiguration;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.fail;

public class SQSWorkerQueueIT
{
    private static SQSTaskCallback callback;
    private static BlockingQueue<CallbackResponse> callbackQueue;
    private static SQSWorkerQueueConfiguration sqsWorkerQueueConfiguration;
    private static SQSConfiguration sqsConfiguration;
    private static SQSWorkerQueue sqsWorkerQueue;
    private static SqsClient sqsClient;
    private static final SQSClientProviderImpl connectionProvider = new SQSClientProviderImpl();

    private final static int visibilityTimeout = 5;
    private final static int longPollInterval = 2;

    private static String inputQueueUrl;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        callback = new SQSTaskCallback();

        sqsConfiguration = new SQSConfiguration();
        sqsConfiguration.setSqsProtocol("http");
        sqsConfiguration.setSqsHost("localhost");
        sqsConfiguration.setSqsPort(19324);
        sqsConfiguration.setSqsRegion("us-east-1");
        sqsConfiguration.setSqsAccessKey("x");
        sqsConfiguration.setSqsSecretAccessKey("x");

        sqsWorkerQueueConfiguration = new SQSWorkerQueueConfiguration();
        sqsWorkerQueueConfiguration.setSQSConfiguration(sqsConfiguration);
        sqsWorkerQueueConfiguration.setInputQueue("worker-in");
        sqsWorkerQueueConfiguration.setVisibilityTimeout(visibilityTimeout);
        sqsWorkerQueueConfiguration.setLongPollInterval(longPollInterval);

        sqsWorkerQueue = new SQSWorkerQueue(sqsWorkerQueueConfiguration);
        sqsWorkerQueue.start(callback);

        sqsClient = connectionProvider.getSqsClient(sqsConfiguration);
        callbackQueue = callback.getCallbackQueue();
        inputQueueUrl = SQSUtil.getQueueUrl(sqsClient, sqsWorkerQueueConfiguration.getInputQueue());
    }

    @Test
    public void testInputQueueIsCreated()
    {
        try {
            final var getQueueUrlRequest = GetQueueUrlRequest.builder()
                    .queueName(sqsWorkerQueueConfiguration.getInputQueue())
                    .build();
            sqsClient.getQueueUrl(getQueueUrlRequest);
        } catch (final Exception e) {
            fail("The input queue was not created:" + e.getMessage());
        }
    }

    @Test
    public void testPublish() throws Exception
    {
        final var msgBody = "Hello-World";
        sendMessage(msgBody);

        final var msg = callbackQueue.poll(30, TimeUnit.SECONDS);
        final var body = msg.body();
        Assert.assertEquals(body, msgBody, "Message was not as expected");

        deleteMessage(msg.taskInformation().getInboundMessageId());
    }

    @Test
    public void testReceiveMultipleMessages() throws Exception
    {
        final var msg1 = "Message1";
        final var msg2 = "Message2";
        sendMessage(msg1, msg2);

        var receiveMessageResult = new ArrayList<CallbackResponse>();
        receiveMessageResult.add(callbackQueue.poll(30, TimeUnit.SECONDS));
        receiveMessageResult.add(callbackQueue.poll(30, TimeUnit.SECONDS));

        var messages = receiveMessageResult.stream().map(m -> m.body()).toList();

        Assert.assertTrue(messages.contains(msg1), "Message 1 was not found");
        Assert.assertTrue(messages.contains(msg2), "Message 2 was not found");
        for (final var msg : receiveMessageResult) {
            deleteMessage(msg.taskInformation().getInboundMessageId());
        }
    }

    @Test
    public void testMessageIsRedeliveredAfterVisibilityTimeoutExpires() throws Exception
    {
        final var msgBody = "Redelivery";
        sendMessage(msgBody);

        final var msg = callbackQueue.poll(30, TimeUnit.SECONDS);
        final var body = msg.body();
        Assert.assertEquals(body, msgBody, "Message was not as expected");

        Thread.sleep(visibilityTimeout * 1100);

        final var redeliveredMsg = callbackQueue.poll(30, TimeUnit.SECONDS);;
        final var redeliveredBody = redeliveredMsg.body();
        Assert.assertEquals(body, redeliveredBody, "Redelivered message was not as expected");

        deleteMessage(redeliveredMsg.taskInformation().getInboundMessageId());
    }

    public static void sendMessage(final String... messages)
    {
        try {
            for (final String message : messages) {
                final var sendRequest = SendMessageRequest.builder()
                        .queueUrl(inputQueueUrl)
                        .messageBody(message)
                        .build();
                sqsClient.sendMessage(sendRequest);
            }
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    private static void deleteMessage(final String receiptHandle)
    {
        final var deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(inputQueueUrl)
                .receiptHandle(receiptHandle)
                .build();
        sqsClient.deleteMessage(deleteRequest);
    }
}
