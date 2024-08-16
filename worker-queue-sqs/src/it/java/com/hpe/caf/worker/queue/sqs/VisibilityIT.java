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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.getWorkerWrapper;
import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.purgeQueue;
import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.sendMessages;
import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.sendSingleMessagesWithDelays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

public class VisibilityIT
{
    /**
     * Testing when we send one unique message per second for N seconds with a visibility timeout of N seconds.
     * The visibility extender should not fall behind and only 100 unique messages should be delivered.
     * @throws Exception
     */
    @Test
    public void testExtensionDoesNotMissExpiringMessages() throws Exception
    {
        final var inputQueue = "keep-extending-visibility";
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                100,
                20,
                1,
                1000,
                600);

        sendSingleMessagesWithDelays(workerWrapper.sqsClient, workerWrapper.inputQueueUrl, 100, 1);

        final Set<String> msgBodies = new HashSet<>();
        var msg = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
        msgBodies.add(msg.body());
        while (msg != null) {
            msg = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
            if (msg != null) {
                msgBodies.add(msg.body());
            }
        }

        try {
            // Should be 100 unique messages
            assertEquals(100, msgBodies.size());

            // No further messages should be received.
            msg = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
            int attempts = 0;
            while (msg != null) {
                attempts++;
                msg = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
                if (msg != null) {
                    fail("Should not have received anything");
                }
                if (attempts >= 10) break;
            }
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testVisibilityTimeoutExtensionIsCancelled() throws Exception
    {
        final var inputQueue = "stop-extend-visibility";
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                30,
                20,
                1,
                1000,
                600);
        final var msgBody = "hello-world";
        sendMessages(workerWrapper, msgBody);

        final var msg = workerWrapper.callbackQueue.poll(3, TimeUnit.SECONDS);

        // Message should NOT be redelivered
        final var notRedelivered = workerWrapper.callbackQueue.poll(35, TimeUnit.SECONDS);

        workerWrapper.sqsWorkerQueue.discardTask(msg.taskInformation());

        final var delivered = workerWrapper.callbackQueue.poll(2, TimeUnit.MINUTES);

        try {
            assertNotNull(msg, "Original Message should have been delivered");
            assertNull(notRedelivered, "Message should not be redelivered");
            assertNotNull(delivered, "Should have been delivered");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }
}
