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

import com.hpe.caf.api.HealthResult;
import com.hpe.caf.worker.queue.sqs.util.CallbackResponse;
import com.hpe.caf.worker.queue.sqs.util.SQSUtil;
import com.hpe.caf.worker.queue.sqs.util.WrapperConfig;
import org.testng.Assert;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.sendMessages;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.sendSingleMessagesWithDelays;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

public class SQSWorkerQueueIT extends TestContainer
{
    @Test
    public void testPublishAcknowledgeDelete() throws Exception
    {
        final var inputQueue = "test-publish-ack-del";
        final var timeout = 5;
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                inputQueue,
                new WrapperConfig(
                        timeout,
                        timeout,
                        1,
                        1000,
                        1000,
                        1000
                ));
        final var msgBody = "Hello-World";
        final var metricsReporter = workerWrapper.metricsReporter;

        sendMessages(workerWrapper, msgBody);
        try {
            final var msg = workerWrapper.callbackQueue.poll(10, TimeUnit.SECONDS);
            assertNotNull(msg, "A Message should have been received.");

            // Publish
            final var lastMessage = true;
            workerWrapper.sqsWorkerQueue.publish(
                    msg.taskInformation(),
                    msg.body().getBytes(StandardCharsets.UTF_8),
                    "target-queue",
                    new HashMap<>(),
                    lastMessage
            );

            // Ack
            workerWrapper.sqsWorkerQueue.acknowledgeTask(msg.taskInformation());

            // Stop watching
            workerWrapper.sqsWorkerQueue.discardTask(msg.taskInformation());

            // The delete thread sleeps for 5 seconds between iterations.
            final var nullMessage = workerWrapper.callbackQueue.poll(timeout * 2, TimeUnit.SECONDS);
            assertNull(nullMessage, "A Message should not have been received.");

            assertEquals("Metrics should only have reported a single message",
                    1, metricsReporter.getMessagesReceived());
            assertEquals("Metrics should not have reported errors",
                    0, metricsReporter.getQueueErrors());
            assertEquals("Metrics should have reported one dropped messages",
                    1, metricsReporter.getMessagesDropped());
            assertEquals("Metrics should not have reported rejected messages",
                    0, metricsReporter.getMessagesRejected());
        } finally {
            workerWrapper.sqsWorkerQueue.shutdown();
        }
    }

    @Test
    public void testAcknowledgePublishDelete() throws Exception
    {
        final var inputQueue = "test-ack-publish-del";
        final var timeout = 5;
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                inputQueue,
                new WrapperConfig(
                        timeout,
                        timeout,
                        1,
                        1000,
                        1000,
                        1000
                ));
        final var msgBody = "Hello-World";
        final var metricsReporter = workerWrapper.metricsReporter;

        sendMessages(workerWrapper, msgBody);
        try {
            final var msg = workerWrapper.callbackQueue.poll(10, TimeUnit.SECONDS);
            assertNotNull(msg, "A Message should have been received.");

            // Ack
            workerWrapper.sqsWorkerQueue.acknowledgeTask(msg.taskInformation());

            // Publish
            final var lastMessage = true;
            workerWrapper.sqsWorkerQueue.publish(
                    msg.taskInformation(),
                    msg.body().getBytes(StandardCharsets.UTF_8),
                    "target-queue",
                    new HashMap<>(),
                    lastMessage
            );

            // Stop watching
            workerWrapper.sqsWorkerQueue.discardTask(msg.taskInformation());

            // The delete thread sleeps for 5 seconds between iterations.
            final var nullMessage = workerWrapper.callbackQueue.poll(timeout * 2, TimeUnit.SECONDS);
            assertNull(nullMessage, "A Message should not have been received.");

            assertEquals("Metrics should only have reported a single message",
                    1, metricsReporter.getMessagesReceived());
            assertEquals("Metrics should not have reported errors",
                    0, metricsReporter.getQueueErrors());
            assertEquals("Metrics should have reported one dropped messages",
                    1, metricsReporter.getMessagesDropped());
            assertEquals("Metrics should not have reported rejected messages",
                    0, metricsReporter.getMessagesRejected());
        } finally {
            workerWrapper.sqsWorkerQueue.shutdown();
        }
    }

    @Test
    public void testInputQueueIsCreated()
    {
        final var inputQueue = "input-queue-created";
        final var workerWrapper = getWorkerWrapper(inputQueue);
        try {
            final var getQueueUrlRequest = GetQueueUrlRequest.builder()
                    .queueName(workerWrapper.workerQueueConfiguration.getInputQueue())
                    .build();
            workerWrapper.sqsClient.getQueueUrl(getQueueUrlRequest);
        } catch (final Exception e) {
            Assert.fail("The input queue was not created:" + e.getMessage());
        } finally {
            workerWrapper.sqsWorkerQueue.shutdown();
        }
    }

    @Test
    public void testLivenessCheck()
    {
        final var inputQueue = "liveness-check";
        final var workerWrapper = getWorkerWrapper(inputQueue);
        final var result = workerWrapper.sqsWorkerQueue.livenessCheck();
        assertEquals("Expected a healthy response",
                HealthResult.RESULT_HEALTHY.getStatus().name(),
                result.getStatus().name());
        workerWrapper.sqsWorkerQueue.shutdown();
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
            assertNotNull(msg, "A Message should have been received.");
            assertTrue(msg.headers().containsKey(SQSUtil.SOURCE_QUEUE),
                    "Expected header: " + SQSUtil.SOURCE_QUEUE);
            assertEquals("Expected:" + inputQueue, inputQueue, msg.headers().get(SQSUtil.SOURCE_QUEUE).toString());
        } finally {
            workerWrapper.sqsWorkerQueue.shutdown();
        }
    }

    @Test
    public void testDisconnectReconnectIncomingMessages() throws Exception
    {
        final var inputQueue = "test-disconnect";
        final var workerWrapper = getWorkerWrapper(inputQueue);
        final var msgBody = "Hello-World";
        final var metricsReporter = workerWrapper.metricsReporter;
        // Defaults to receive messages, reverse that here.
        workerWrapper.sqsWorkerQueue.disconnectIncoming();

        // Ensure flag is unset
        final var msg = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
        assertNull(msg, "A Message should not have been received.");

        sendMessages(workerWrapper, msgBody);
        try {
            assertFalse("Expected not to be receiving", workerWrapper.isReceiving());

            final var stillNull = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
            assertNull(stillNull, "A Message should still not have been received.");

            workerWrapper.sqsWorkerQueue.reconnectIncoming();
            final var reconnectedMsg = workerWrapper.callbackQueue.poll(10, TimeUnit.SECONDS);
            assertNotNull(reconnectedMsg, "A Message should have been received.");
            final var body = reconnectedMsg.body();

            assertTrue(workerWrapper.isReceiving(), "Expected to be receiving");

            assertFalse(reconnectedMsg.taskInformation().isPoison());
            assertEquals("Message was not as expected", msgBody, body);
            assertEquals("Metrics should only have reported a single message",
                    1, metricsReporter.getMessagesReceived());
            assertEquals("Metrics should not have reported errors",
                    0, metricsReporter.getQueueErrors());
            assertEquals("Metrics should not have reported dropped messages",
                    0, metricsReporter.getMessagesDropped());
            assertEquals("Metrics should not have reported rejected messages",
                    0, metricsReporter.getMessagesRejected());
        } finally {
            workerWrapper.sqsWorkerQueue.shutdown();
        }
    }

    @Test
    public void testInvalidTaskException()
    {
        final var inputQueue = "test-invalid-task";
        final var retryQueue = "retry-invalid-task";
        final var workerWrapper = getWorkerWrapper(inputQueue, retryQueue);
        final var msgBody = "INVALID"; // Causes exception to be thrown
        final var metricsReporter = workerWrapper.metricsReporter;

        final var rejectQueueUrl = SQSUtil.getQueueUrl(workerWrapper.sqsClient, retryQueue);

        sendMessages(workerWrapper, msgBody);
        try {
            final var receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(rejectQueueUrl)
                    .maxNumberOfMessages(1)
                    .waitTimeSeconds(5)
                    .messageSystemAttributeNames(MessageSystemAttributeName.ALL)
                    .messageAttributeNames(SQSUtil.ALL_ATTRIBUTES)
                    .build();
            final var result = workerWrapper.sqsClient.receiveMessage(receiveRequest).messages();
            assertEquals("should only have received a single message", 1, result.size());
            final var body = result.get(0).body();
            assertEquals("Message was not as expected", msgBody, body);
            assertEquals("Metrics should only have reported a single message",
                    1, metricsReporter.getMessagesReceived());
            assertEquals("Metrics should not have reported errors",
                    0, metricsReporter.getQueueErrors());
            assertEquals("Metrics should not have reported dropped messages",
                    0, metricsReporter.getMessagesDropped());
            assertEquals("Metrics should not have reported rejected messages",
                    0, metricsReporter.getMessagesRejected());
        } finally {
            workerWrapper.sqsWorkerQueue.shutdown();
        }
    }

    @Test
    public void testTaskRejectedException()
    {
        final var inputQueue = "test-rejected-task";
        final var retryQueue = "retry-rejected-task";
        final var workerWrapper = getWorkerWrapper(inputQueue, retryQueue);
        final var msgBody = "REJECTED"; // Causes exception to be thrown

        final var rejectQueueUrl = SQSUtil.getQueueUrl(workerWrapper.sqsClient, retryQueue);
        final var metricsReporter = workerWrapper.metricsReporter;
        sendMessages(workerWrapper, msgBody);
        try {
            // No listener for reject queue
            final var receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(rejectQueueUrl)
                    .maxNumberOfMessages(1)
                    .waitTimeSeconds(5)
                    .messageSystemAttributeNames(MessageSystemAttributeName.ALL)
                    .messageAttributeNames(SQSUtil.ALL_ATTRIBUTES)
                    .build();
            final var result = workerWrapper.sqsClient.receiveMessage(receiveRequest).messages();
            assertEquals("should not have received a message on reject queue", 0, result.size());

            assertEquals("Metrics should only have reported a single message",
                    1, metricsReporter.getMessagesReceived());
            assertEquals("Metrics should not have reported errors",
                    0, metricsReporter.getQueueErrors());
            assertEquals("Metrics should not have reported dropped messages",
                    0, metricsReporter.getMessagesDropped());
            assertEquals("Metrics should have reported 1 rejected messages",
                    1, metricsReporter.getMessagesRejected());
        } finally {
            workerWrapper.sqsWorkerQueue.shutdown();
        }
    }

    @Test
    public void testMaxInflightMessages() throws Exception
    {
        final var inputQueue = "test-max-inflight-messages";
        final int timeout;
        final int maxInflightMessages = 2;
        final int maxMessagesToRead = 1;
        final int numberOfMessagesSent = timeout = 5;
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                inputQueue,
                new WrapperConfig(
                        timeout,
                        1,
                        maxMessagesToRead,
                        1000,
                        1000,
                        maxInflightMessages
                ));
        sendSingleMessagesWithDelays(workerWrapper.sqsClient, workerWrapper.inputQueueUrl, numberOfMessagesSent, 0L);

        try {
            final List<CallbackResponse> callbackResponses = new ArrayList<>();
            CallbackResponse msg;
            do {
                msg = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
                if (msg != null) {
                    callbackResponses.add(msg);
                }
            } while (msg != null);

            Assert.assertEquals(callbackResponses.size(), maxInflightMessages);

            // No further messages should be received till an inflight message is republished and acked.
            for (int i = 0; i < 2; i++) {
                msg = workerWrapper.callbackQueue.poll(timeout, TimeUnit.SECONDS);
                assertNull(msg, "A Message should not have been received.");
            }

            // Publish
            final var lastMessage = true;
            final var lastReceivedMsg = callbackResponses.get(0);
            workerWrapper.sqsWorkerQueue.publish(
                    lastReceivedMsg.taskInformation(),
                    lastReceivedMsg.body().getBytes(StandardCharsets.UTF_8),
                    "next-queue",
                    new HashMap<>(),
                    lastMessage
            );

            // Ack
            workerWrapper.sqsWorkerQueue.acknowledgeTask(lastReceivedMsg.taskInformation());

            // Now we should receive one more message
            final var oneMoreMessage = workerWrapper.callbackQueue.poll(1, TimeUnit.MINUTES);
            assertNotNull(oneMoreMessage, "A Message should have been received.");
        } finally {
            workerWrapper.sqsWorkerQueue.shutdown();
        }
    }
}
