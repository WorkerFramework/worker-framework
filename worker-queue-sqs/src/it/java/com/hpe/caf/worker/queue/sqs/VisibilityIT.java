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

import java.util.concurrent.TimeUnit;

import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.getWorkerWrapper;
import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.purgeQueue;
import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.sendMessages;

public class VisibilityIT
{
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

        final var firstDelivery = workerWrapper.callbackQueue.poll(3, TimeUnit.SECONDS);
        var redeliveredMsg = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);
        try {
            Assert.assertNotNull(firstDelivery, "Message should have been delivered");
            Assert.assertNull(redeliveredMsg, "Message should not have been redelivered");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testVisibilityTimeoutGetsExtended() throws Exception
    {
        var inputQueue = "extend-visibility";
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                10,
                20,
                1,
                1000,
                600);
        final var msgBody = "hello-world";
        sendMessages(workerWrapper, msgBody);

        final var msg = workerWrapper.callbackQueue.poll(3, TimeUnit.SECONDS);

        // Message should be redelivered
        var redelivered = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);

        try {
            Assert.assertNotNull(msg, "Original Message should have been delivered");
            Assert.assertNull(redelivered, "Message should not be redelivered");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }

    @Test
    public void testVisibilityTimeoutExtensionIsCancelled() throws Exception
    {
        var inputQueue = "stop-extend-visibility";
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                10,
                20,
                1,
                1000,
                600);
        final var msgBody = "hello-world";
        sendMessages(workerWrapper, msgBody);

        final var msg = workerWrapper.callbackQueue.poll(3, TimeUnit.SECONDS);

        // Message should NOT be redelivered
        var notRedelivered = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);

        workerWrapper.sqsWorkerQueue.discardTask(msg.taskInformation());

        // DDD Something wrong here, message should now be delivered
        var delivered = workerWrapper.callbackQueue.poll(2, TimeUnit.MINUTES);

        try {
            Assert.assertNotNull(msg, "Original Message should have been delivered");
            Assert.assertNull(notRedelivered, "Message should not be redelivered");
            Assert.assertNotNull(delivered, "Should have been delivered");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }
}
