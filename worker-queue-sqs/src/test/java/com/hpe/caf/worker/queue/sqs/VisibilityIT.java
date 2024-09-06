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
import com.hpe.caf.worker.queue.sqs.util.SQSUtil;
import com.hpe.caf.worker.queue.sqs.util.WrapperConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.deleteMessage;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.purgeQueue;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.sendMessages;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.sendSingleMessagesWithDelays;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class VisibilityIT extends TestContainer
{
    private static final Logger LOG = LoggerFactory.getLogger(VisibilityIT.class);

    /**
     * Testing when we send one unique message per second for N seconds with a visibility timeout of N seconds.
     * The visibility extender should not fall behind and only N unique messages should be delivered.
     *
     * @throws Exception error in test
     */
    @Test
    public void testExtendingMultipleMessages() throws Exception
    {
        final var inputQueue = "keep-extending-visibility";
        final int timeout;
        final int numberOfMessages = timeout = 10;
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                inputQueue,
                new WrapperConfig(
                        timeout,
                        1,
                        10,
                        1000,
                        1000
                ));
        sendSingleMessagesWithDelays(workerWrapper.sqsClient, workerWrapper.inputQueueUrl, numberOfMessages, 2L);

        try {
            final List<String> msgBodies = new ArrayList<>();
            var msg = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
            assertNotNull(msg, "A Message should have been received.");
            msgBodies.add(msg.body());
            while (msg != null) {
                msg = workerWrapper.callbackQueue.poll(2, TimeUnit.SECONDS);
                if (msg != null) {
                    msgBodies.add(msg.body());
                }
            }
            // Should be N unique messages
            assertEquals(msgBodies.stream().distinct().count(), numberOfMessages);

            LOG.debug("Should not get messages after this");
            // No further messages should be received.
            msg = workerWrapper.callbackQueue.poll(1, TimeUnit.SECONDS);
            assertNull(msg, "A Message should NOT have been received.");
            int attempts = 0;
            while (msg == null) {
                msg = workerWrapper.callbackQueue.poll(timeout, TimeUnit.SECONDS);
                if (msg != null) {
                    fail("Should not have received anything");
                }
                if (++attempts >= numberOfMessages / 2) break;
            }
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testVisibilityTimeoutExtensionIsCancelled() throws Exception
    {
        final var inputQueue = "stop-extend-visibility";
        final int timeout = 10;
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                inputQueue,
                new WrapperConfig(
                        timeout,
                        timeout,
                        1,
                        1000,
                        1000
                ));
        final var msgBody = "hello-world";
        sendMessages(workerWrapper, msgBody);
        final var metricsReporter = workerWrapper.metricsReporter;
        try {
            final var msg = workerWrapper.callbackQueue.poll(timeout, TimeUnit.SECONDS);
            assertNotNull(msg, "Original Message should have been delivered");
            final List<CallbackResponse> remainingResponses = new ArrayList<>();
            workerWrapper.callbackQueue.drainTo(remainingResponses);
            assertEquals(remainingResponses.size(), 0, "Should not have received any responses");

            // Message should NOT be redelivered
            final var notRedelivered = workerWrapper.callbackQueue.poll(timeout * 4, TimeUnit.SECONDS);
            assertNull(notRedelivered, "Message should not be redelivered");

            workerWrapper.sqsWorkerQueue.discardTask(msg.taskInformation());

            final CallbackResponse redelivered = workerWrapper.callbackQueue.poll(timeout * 6, TimeUnit.SECONDS);
            final CallbackResponse redeliveredFromDLQ = workerWrapper.callbackDLQ.poll(10, TimeUnit.SECONDS);
            assertNull(redeliveredFromDLQ, "Should not have been redelivered from DLQ");
            assertNotNull(redelivered, "Should have been redelivered");

            // DDD 1st & 2nd delivery?
            AssertJUnit.assertEquals("Metrics should have reported 2 messages",
                    2, metricsReporter.getMessagesReceived());
            AssertJUnit.assertEquals("Metrics should not have reported errors",
                    0, metricsReporter.getQueueErrors());
            AssertJUnit.assertEquals("Metrics should have reported 1 dropped messages",
                    1, metricsReporter.getMessagesDropped());
            AssertJUnit.assertEquals("Metrics should not have reported rejected messages",
                    0, metricsReporter.getMessagesRejected());
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testAttemptToChangeVisibilityWithInvalidReceiptHandleDoesNotCrashProcess() throws Exception
    {
        final var inputQueue = "test-visibility-after-retention-period-expires";
        final var workerWrapper = getWorkerWrapper(inputQueue);
        final var msgBody = "Hello-World";
        final var metricsReporter = workerWrapper.metricsReporter;
        try {
            sendMessages(workerWrapper, msgBody);

            final var msg = workerWrapper.callbackQueue.poll(20, TimeUnit.SECONDS);
            assertNotNull(msg, "Message should have been received.");

            final var receiptHandle = msg.taskInformation().getReceiptHandle();

            deleteMessage(workerWrapper.sqsClient, msg.taskInformation());

            final var visibilityRequestEntry = ChangeMessageVisibilityBatchRequestEntry.builder()
                    .id("1")
                    .receiptHandle(receiptHandle)
                    .visibilityTimeout(200000)
                    .build();

            // Intentionally sending batch of one
            final var req = ChangeMessageVisibilityBatchRequest.builder()
                    .entries(Collections.singleton(visibilityRequestEntry))
                    .queueUrl(workerWrapper.inputQueueUrl)
                    .build();

            try {
                final var result = workerWrapper.sqsClient.changeMessageVisibilityBatch(req);
                assertNotNull(result, "Message should have been received.");

                AssertJUnit.assertEquals("Metrics should only have reported a single message",
                        1, metricsReporter.getMessagesReceived());
                AssertJUnit.assertEquals("Metrics should not have reported errors",
                        0, metricsReporter.getQueueErrors());
                AssertJUnit.assertEquals("Metrics should not have reported dropped messages",
                        0, metricsReporter.getMessagesDropped());
                AssertJUnit.assertEquals("Metrics should not have reported rejected messages",
                        0, metricsReporter.getMessagesRejected());
            } catch (final Exception e) {
                fail("Unexpected exception when changing visibility for expired receipt handle " + e);
            }
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testExtensionForSingleMessages() throws Exception
    {
        final var inputQueue = "test-extending-single-message";
        final var timeout = 10;
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                inputQueue,
                new WrapperConfig(
                        timeout,
                        1,
                        1,
                        1000,
                        1000
                ));
        final int messagesToSend = 1;
        sendSingleMessagesWithDelays(workerWrapper.sqsClient, workerWrapper.inputQueueUrl, messagesToSend, 1L);

        try {
            CallbackResponse response;
            do {
                response = workerWrapper.callbackQueue.poll(1, TimeUnit.SECONDS);
            } while (response == null);
            final var gotFirst = Instant.now();
            int attempts = 0;
            do {
                response = workerWrapper.callbackQueue.poll(1, TimeUnit.SECONDS);
                if (++attempts > (timeout * 2)) {
                    break;
                }
            } while (response == null);
            if (response != null) {
                final var gotNext = Instant.now();
                final Duration res = Duration.between(gotFirst, gotNext);
                fail("Message got redelivered after " + res.getSeconds() + " seconds");
            }
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    /**
     * Checking container visibility timeout works outside of application.
     *
     * @throws Exception
     */
    @Test
    public void testVisibilityTimeoutWorks() throws Exception
    {
        final var inputQueue = "extend-visibility";
        final int timeout = 10;
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                inputQueue,
                new WrapperConfig(
                        timeout,
                        1,
                        1,
                        1000,
                        1000
                )
        );

        final var queueCfg = workerWrapper.workerQueueConfiguration;
        final var testQueue = "check-visibility";
        final var testQueueInfo = SQSUtil.createQueue(workerWrapper.sqsClient, testQueue, queueCfg);

        sendMessages(workerWrapper.sqsClient, testQueueInfo.url(), new HashMap<>(), "Hello World");

        try {
            final var receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(testQueueInfo.url())
                    .maxNumberOfMessages(1)
                    .waitTimeSeconds(0)
                    .messageSystemAttributeNames(MessageSystemAttributeName.ALL)
                    .messageAttributeNames(SQSUtil.ALL_ATTRIBUTES)
                    .build();
            ReceiveMessageResponse receiveMessageResult;
            do {
                receiveMessageResult = workerWrapper.sqsClient.receiveMessage(receiveRequest);
            } while (!receiveMessageResult.hasMessages());
            final var gotFirst = Instant.now();
            do {
                receiveMessageResult = workerWrapper.sqsClient.receiveMessage(receiveRequest);
            } while (!receiveMessageResult.hasMessages());
            final var gotNext = Instant.now();
            final Duration res = Duration.between(gotFirst, gotNext);
            assertTrue(res.getSeconds() >= timeout && res.getSeconds() < timeout * 2,
                    "Expected close to timeout, but timeout was " + res.getSeconds());

        } finally {
            purgeQueue(workerWrapper.sqsClient, testQueueInfo.url());
        }
    }
}
