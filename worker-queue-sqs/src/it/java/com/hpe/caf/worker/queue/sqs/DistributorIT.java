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

import com.hpe.caf.worker.queue.sqs.distributor.MessageDistributor;
import com.hpe.caf.worker.queue.sqs.util.CallbackResponse;
import com.hpe.caf.worker.queue.sqs.util.SQSUtil;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.getWorkerWrapper;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.purgeQueue;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.sendMessagesInBatches;
import static org.testng.Assert.assertEquals;

public class DistributorIT
{
    @Test
    public void testRedistributeMessagesWhenNoneExistDoesNotThrowError() throws Exception
    {
        var destinationQueue = "test-redistribute-none-destination";
        var sourceQueue = "test-redistribute-none-source";
        var numberOfMessages = 0;
        var numberOfMessagesToMove = 15;
        var expectedNumberOfMessagesToMove = 0;
        runRedistributionTest(
                sourceQueue,
                destinationQueue,
                numberOfMessages,
                numberOfMessagesToMove,
                expectedNumberOfMessagesToMove
        );
    }

    @Test
    public void testRedistributeMessagesInSingleBatch() throws Exception
    {
        var destinationQueue = "test-redistribute-destination";
        var sourceQueue = "test-redistribute-source";
        var numberOfMessages = 100;
        var numberOfMessagesToMove = 15;
        var expectedNumberOfMessagesToMove = 10;
        runRedistributionTest(
                sourceQueue,
                destinationQueue,
                numberOfMessages,
                numberOfMessagesToMove,
                expectedNumberOfMessagesToMove
        );
    }

    @Test
    public void testRedistributeMessagesInMultipleBatches() throws Exception
    {
        var destinationQueue = "test-redistribute-multi-destination";
        var sourceQueue = "test-redistribute-multi-source";
        var numberOfMessages = 100;
        var numberOfMessagesToMove = 100;
        var expectedNumberOfMessagesToMove = 100;
        runRedistributionTest(
                sourceQueue,
                destinationQueue,
                numberOfMessages,
                numberOfMessagesToMove,
                expectedNumberOfMessagesToMove
        );
    }

    private void runRedistributionTest(
            final String sourceQueue,
            final String destinationQueue,
            final int numberOfMessages,
            final int numberOfMessagesToMove,
            final int expectedMessagesToBeMoved
    ) throws Exception
    {
        final var workerWrapper = getWorkerWrapper(destinationQueue);

        final var createQueueRequest = CreateQueueRequest.builder()
                .queueName(sourceQueue)
                .build();
        final var sourceQueueUrl = workerWrapper.sqsClient.createQueue(createQueueRequest).queueUrl();

        sendMessagesInBatches(workerWrapper.sqsClient, sourceQueueUrl, numberOfMessages);

        final var distributor = new MessageDistributor(
                SQSUtil.getSqsClient(workerWrapper.sqsConfiguration),
                sourceQueue,
                destinationQueue
        );

        final var failures = distributor.moveMessages(numberOfMessagesToMove);

        final var receivedMessages = new ArrayList<CallbackResponse>();
        CallbackResponse response;
        do {
            response = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
            if (response != null) {
                receivedMessages.add(response);
            }
        } while (response != null);

        try {
            assertEquals(receivedMessages.size(), expectedMessagesToBeMoved, "Not all messages were moved");
            assertEquals(failures.size(), 0, "Should not have had failures");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
            purgeQueue(workerWrapper.sqsClient, sourceQueueUrl);
        }
    }
}
