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
import com.hpe.caf.api.worker.TaskInformation;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.configs.SQSConfiguration;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import static org.testng.AssertJUnit.fail;

public class SQSWorkerQueueIT
{

    private static TaskCallback callback;
    private static SQSWorkerQueueConfiguration sqsWorkerQueueConfiguration;
    private static SQSConfiguration sqsConfiguration;
    private static SQSWorkerQueue sqsWorkerQueue;
    private static SqsClient sqsClient;
    private static final SqsClientProviderImpl connectionProvider = new SqsClientProviderImpl();

    private final static int visibilityTimeout = 5;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        callback = new TaskCallback()
        {
            @Override
            public void registerNewTask(
                    TaskInformation taskInformation,
                    byte[] taskData,
                    Map<String, Object> headers
            ) throws TaskRejectedException, InvalidTaskException
            {

            }

            @Override
            public void abortTasks()
            {

            }
        };

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
        sqsWorkerQueueConfiguration.setRetryQueue("retry-in");
        sqsWorkerQueueConfiguration.setVisibilityTimeout(visibilityTimeout);

        sqsWorkerQueue = new SQSWorkerQueue(sqsWorkerQueueConfiguration);
        sqsWorkerQueue.start(callback);

        sqsClient = connectionProvider.getSqsClient(sqsConfiguration);;
    }

    @Test
    public void testInputQueueIsCreated()
    {
        try
        {
            final var getQueueUrlRequest = GetQueueUrlRequest.builder()
                    .queueName(sqsWorkerQueueConfiguration.getInputQueue())
                    .build();
            sqsClient.getQueueUrl(getQueueUrlRequest);
        } catch (final Exception e)
        {
            fail("The input queue was not created:" + e.getMessage());
        }
    }

    @Test
    public void testRetryQueueIsCreated()
    {
        try
        {
            final var getQueueUrlRequest = GetQueueUrlRequest.builder()
                    .queueName(sqsWorkerQueueConfiguration.getRetryQueue())
                    .build();
            sqsClient.getQueueUrl(getQueueUrlRequest);
        } catch (final Exception e)
        {
            fail("The retry queue was not created");
        }
    }

    @Test
    public void testPublishToAnExistingQueueDoesNotThrowException()
    {
        try
        {
            sendMessage(sqsWorkerQueue, sqsWorkerQueueConfiguration.getInputQueue(), "Hello-World");
            final var receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(SQSUtil.getQueueUrl(sqsClient, sqsWorkerQueueConfiguration.getInputQueue()))
                    .build();
            final var receiveMessageResult = sqsClient.receiveMessage(receiveRequest).messages();
            final var msg = receiveMessageResult.get(0);
            deleteMessage(sqsWorkerQueueConfiguration.getInputQueue(), msg.receiptHandle());
        } catch (final Exception e)
        {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testPublish() throws Exception
    {
        final var queueName = "Publish";
        final var msgBody = "Hello-World";
        sendMessage(sqsWorkerQueue, queueName, msgBody);
        Thread.sleep(5000);
        final var receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(SQSUtil.getQueueUrl(sqsClient, queueName))
                .build();
        final var receiveMessageResult = sqsClient.receiveMessage(receiveRequest).messages();
        Assert.assertEquals(receiveMessageResult.size(), 1, "Wrong number of receiveMessageResult");
        final var msg = receiveMessageResult.get(0);
        final var body = msg.body();
        Assert.assertEquals(body, msgBody, "Message was not as expected");

        deleteMessage(queueName, msg.receiptHandle());
    }

    @Test
    public void testMessageIsRedeliveredAfterVisibilityTimeoutExpires() throws Exception
    {
        final var queueName = "ExpiredVisibilityTimeout";
        final var msgBody = "Redelivery";
        sendMessage(sqsWorkerQueue, queueName, msgBody);
        Thread.sleep(5000);
        final var receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(SQSUtil.getQueueUrl(sqsClient, queueName))
                .build();
        final var receiveMessageResult = sqsClient.receiveMessage(receiveRequest).messages();
        Assert.assertEquals(receiveMessageResult.size(), 1, "Wrong number of receiveMessageResult");
        final var msg = receiveMessageResult.get(0);
        final var body = msg.body();
        Assert.assertEquals(body, msgBody, "Message was not as expected");

        Thread.sleep(5000 + (visibilityTimeout * 1000));
        final var redeliveredMessageResult = sqsClient.receiveMessage(receiveRequest).messages();
        Assert.assertEquals(redeliveredMessageResult.size(), 1, "Wrong number of receiveMessageResult");
        final var redeliveredMsg = redeliveredMessageResult.get(0);
        final var redeliveredBody = redeliveredMsg.body();
        Assert.assertEquals(redeliveredBody, msgBody, "Message was not as expected");

        deleteMessage(queueName, redeliveredMsg.receiptHandle());
    }

    @Test
    public void testThatVisibleMessageCanBeDeleted() throws Exception
    {
        final var queueName = "DeleteWhenVisible";
        final var msgBody = "DeleteMeIfVisible";
        sendMessage(sqsWorkerQueue, queueName, msgBody);
        Thread.sleep(5000);

        final var receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(SQSUtil.getQueueUrl(sqsClient, queueName))
                .build();
        final var result = sqsClient.receiveMessage(receiveRequest).messages();
        Assert.assertEquals(result.size(), 1, "Wrong number of receiveMessageResult");
        final var msg = result.get(0);
        final var body = msg.body();
        Assert.assertEquals(body, msgBody, "Message was not as expected");
        Thread.sleep(5000 + (visibilityTimeout * 1000));

        deleteMessage(queueName, msg.receiptHandle());
    }

    @Test
    public void testMessageIsNotRedeliveredDuringVisibilityTimeout() throws Exception
    {
        final var queueName = "UnexpiredVisibilityTimeout";
        final var msgBody = "No-Redelivery";
        sendMessage(sqsWorkerQueue, queueName, msgBody);
        Thread.sleep(5000);
        final var receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(SQSUtil.getQueueUrl(sqsClient, queueName))
                .build();
        final var receiveMessageResult = sqsClient.receiveMessage(receiveRequest).messages();
        Assert.assertEquals(receiveMessageResult.size(), 1, "Wrong number of receiveMessageResult");
        final var msg = receiveMessageResult.get(0);
        final var body = msg.body();
        Assert.assertEquals(body, msgBody, "Message was not as expected");

        Thread.sleep((visibilityTimeout * 1000) - 5000);
        final var redeliveredMessageResult = sqsClient.receiveMessage(receiveRequest).messages();
        Assert.assertEquals(redeliveredMessageResult.size(), 0, "Wrong number of receiveMessageResult");

        deleteMessage(queueName, msg.receiptHandle());
    }

    public static void sendMessage(final SQSWorkerQueue sqsWorkerQueue, final String queueUrl, final String message)
    {
        try
        {
            final var taskInfo = new TaskInformation()
            {

                @Override
                public String getInboundMessageId()
                {
                    return "XXX";
                }

                @Override
                public boolean isPoison()
                {
                    return false;
                }
            };

            sqsWorkerQueue.publish(taskInfo, message.getBytes(StandardCharsets.UTF_8), queueUrl, null);
        } catch (final Exception e)
        {
            fail(e.getMessage());
        }
    }

    private static void deleteMessage(final String queueName, final String receiptHandle) throws Exception
    {
        final var queueUrl = SQSUtil.getQueueUrl(sqsClient, queueName);
        final var deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build();
        sqsClient.deleteMessage(deleteRequest);
        Thread.sleep(visibilityTimeout * 1000);

        final var receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .build();
        final var receiveMessageResult = sqsClient.receiveMessage(receiveRequest);
        Assert.assertEquals(receiveMessageResult.messages().size(), 0, "Queue should be empty");
    }
}
