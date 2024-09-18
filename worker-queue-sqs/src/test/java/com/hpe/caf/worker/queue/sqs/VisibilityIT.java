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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.deleteMessage;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.sendMessages;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.sendSingleMessagesWithDelays;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

public class VisibilityIT extends TestContainer
{
    private static final Logger LOG = LoggerFactory.getLogger(VisibilityIT.class);

    @Test
    public void testVisibilityTimeoutExtensionIsCancelled() throws Exception
    {
        final var inputQueue = "stop-extend-visibility";
        final int timeout = 5;
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
        final var msgBody = "hello-world";
        sendMessages(workerWrapper, msgBody);
        final var metricsReporter = workerWrapper.metricsReporter;
        try {
            final var msg = workerWrapper.callbackQueue.poll(timeout, TimeUnit.SECONDS);
            assertNotNull(msg, "Original Message should have been delivered");
            final List<CallbackResponse> remainingResponses = new ArrayList<>();
            workerWrapper.callbackQueue.drainTo(remainingResponses);
            assertEquals(remainingResponses.size(), 0, "Should not have received any responses");

            workerWrapper.sqsWorkerQueue.discardTask(msg.taskInformation());

            final CallbackResponse redelivered = workerWrapper.callbackQueue.poll(timeout*2, TimeUnit.SECONDS);
            final CallbackResponse redeliveredFromDLQ = workerWrapper.callbackDLQ.poll(timeout, TimeUnit.SECONDS);
            assertNull(redeliveredFromDLQ, "Should not have been redelivered from DLQ");
            assertNotNull(redelivered, "Should have been redelivered");

            AssertJUnit.assertEquals("Metrics should have reported 2 messages",
                    2, metricsReporter.getMessagesReceived());
            AssertJUnit.assertEquals("Metrics should not have reported errors",
                    0, metricsReporter.getQueueErrors());
            AssertJUnit.assertEquals("Metrics should have reported 1 dropped messages",
                    1, metricsReporter.getMessagesDropped());
            AssertJUnit.assertEquals("Metrics should not have reported rejected messages",
                    0, metricsReporter.getMessagesRejected());
        } finally {
            workerWrapper.sqsWorkerQueue.shutdown();
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
            workerWrapper.sqsWorkerQueue.shutdown();
        }
    }

    @Test
    public void testExtensionForSingleMessages() throws Exception
    {
        final var inputQueue = "test-extending-single-message";
        final var timeout = 5;
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                inputQueue,
                new WrapperConfig(
                        timeout,
                        1,
                        1,
                        1000,
                        1000,
                        1000
                ));
        sendMessages(workerWrapper, "Hello-world");

        try {
            CallbackResponse response;
            do {
                response = workerWrapper.callbackQueue.poll(1, TimeUnit.SECONDS);
            } while (response == null);
            final var gotFirst = Instant.now();

            for (int i = 0; i < 2; i++) {
                response = workerWrapper.callbackQueue.poll(timeout, TimeUnit.SECONDS);

                if (response != null) {
                    final var gotNext = Instant.now();
                    final Duration res = Duration.between(gotFirst, gotNext);
                    fail("Message got redelivered after " + res.getSeconds() + " seconds");
                }
            }
        } finally {
            workerWrapper.sqsWorkerQueue.shutdown();
        }
    }

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
        final int numberOfMessages = timeout = 5;
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                inputQueue,
                new WrapperConfig(
                        timeout,
                        2,
                        10,
                        1000,
                        1000,
                        1000
                ));
        sendSingleMessagesWithDelays(workerWrapper.sqsClient, workerWrapper.inputQueueUrl, numberOfMessages, 1L);

        try {
            final List<String> msgBodies = new ArrayList<>();
            var msg = workerWrapper.callbackQueue.poll(2, TimeUnit.SECONDS);
            assertNotNull(msg, "A Message should have been received.");
            msgBodies.add(msg.body());
            while (msg != null) {
                msg = workerWrapper.callbackQueue.poll(2, TimeUnit.SECONDS);
                if (msg != null) {
                    msgBodies.add(msg.body());
                }
            }
            // Should be N unique messages proving all received
            assertEquals(msgBodies.stream().distinct().count(), numberOfMessages);

            // No further messages should be received.
            for (int i = 0; i < 2; i++) {
                msg = workerWrapper.callbackQueue.poll(timeout, TimeUnit.SECONDS);
                assertNull(msg, "A Message should not have been received.");
            }
        } finally {
            workerWrapper.sqsWorkerQueue.shutdown();
        }
    }
}
