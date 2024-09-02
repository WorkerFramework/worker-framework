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

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.deleteMessage;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.getWorkerWrapper;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.purgeQueue;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.sendMessages;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.sendSingleMessagesWithDelays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

public class VisibilityIT
{
    /**
     * Testing when we send one unique message per second for N seconds with a visibility timeout of N seconds.
     * The visibility extender should not fall behind and only 100 unique messages should be delivered.
     *
     * @throws Exception error in test
     */
    @Test
    public void testExtensionDoesNotMissExpiringMessages() throws Exception
    {
        final var inputQueue = "keep-extending-visibility";
        final var workerWrapper = getWorkerWrapper(inputQueue);

        sendSingleMessagesWithDelays(workerWrapper.sqsClient, workerWrapper.inputQueueUrl, 100, 1L);

        try {
            final Set<String> msgBodies = new HashSet<>();
            var msg = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
            assertNotNull(msg, "A Message should have been received.");
            msgBodies.add(msg.body());
            while (msg != null) {
                msg = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
                if (msg != null) {
                    msgBodies.add(msg.body());
                }
            }
            // Should be 100 unique messages
            assertEquals(100, msgBodies.size());

            // No further messages should be received.
            msg = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
            int attempts = 0;
            while (msg != null) {
                msg = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
                if (msg != null) {
                    fail("Should not have received anything");
                }
                if (++attempts >= 10) break;
            }
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testVisibilityTimeoutExtensionIsCancelled() throws Exception
    {
        final var inputQueue = "stop-extend-visibility";
        final var workerWrapper = getWorkerWrapper(inputQueue);
        final var msgBody = "hello-world";
        sendMessages(workerWrapper, msgBody);
        final var metricsReporter = workerWrapper.metricsReporter;
        try {
            final var msg = workerWrapper.callbackQueue.poll(15, TimeUnit.SECONDS);

            // Message should NOT be redelivered
            final var notRedelivered = workerWrapper.callbackQueue.poll(15, TimeUnit.SECONDS);
            assertNotNull(msg, "Original Message should have been delivered");
            workerWrapper.sqsWorkerQueue.discardTask(msg.taskInformation());

            final var delivered = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);
            assertNull(notRedelivered, "Message should not be redelivered");
            assertNotNull(delivered, "Should have been delivered");

            // DDD should we be reporting two messages here,  1st & 2nd delivery?
            // DDD how could we tell.
            assertEquals(2, metricsReporter.getMessagesReceived(),
                    "Metrics should have reported two messages(incl one redelivery");
            assertEquals(0, metricsReporter.getQueueErrors(),
                    "Metrics should not have reported errors");
            assertEquals(1, metricsReporter.getMessagesDropped(),
                    "Metrics should have reported dropped messages");
            assertEquals(0, metricsReporter.getMessagesRejected(),
                    "Metrics should not have reported rejected messages");
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

                assertEquals(1, metricsReporter.getMessagesReceived(),
                        "Metrics should only have reported a single message");
                assertEquals(0, metricsReporter.getQueueErrors(),
                        "Metrics should not have reported errors");
                assertEquals(0, metricsReporter.getMessagesDropped(),
                        "Metrics should not have reported dropped messages");
                assertEquals(0, metricsReporter.getMessagesRejected(),
                        "Metrics should not have reported rejected messages");
            } catch (final Exception e) {
                fail("Unexpected exception when changing visibility for expired receipt handle", e);
            }
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }
}
