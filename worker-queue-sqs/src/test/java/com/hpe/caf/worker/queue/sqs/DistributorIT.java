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
import com.hpe.caf.worker.queue.sqs.util.SQSUtil;
import com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.sendMessagesInBatches;
import static org.testng.Assert.assertEquals;

public class DistributorIT extends TestContainer
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
        final var sqsClient = SQSUtil.getSqsClient(WorkerQueueWrapper.getSqsConfig(container));

        final var createQueueRequest = CreateQueueRequest.builder()
                .queueName(sourceQueue)
                .build();
        final var sourceQueueUrl = sqsClient.createQueue(createQueueRequest).queueUrl();

        final var createDestinationQueueRequest = CreateQueueRequest.builder()
                .queueName(destinationQueue)
                .build();
        final var destinationQueueUrl = sqsClient.createQueue(createDestinationQueueRequest).queueUrl();

        sendMessagesInBatches(sqsClient, sourceQueueUrl, numberOfMessages);

        final var distributor = new MessageDistributor(
                sqsClient,
                sourceQueue,
                destinationQueue
        );

        final var failures = distributor.moveMessages(numberOfMessagesToMove);

        final var destinationRequest = ReceiveMessageRequest.builder()
                .queueUrl(destinationQueueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(20)
                .build();

        int responseCount = 0;
        do {
            final var receiveMessageResponse = sqsClient.receiveMessage(destinationRequest).messages();
            responseCount += receiveMessageResponse.size();
            if (receiveMessageResponse.size() == 0) {
                break;
            }
        } while (true);

        failures.forEach(f -> System.out.println(f.message()));
        assertEquals(failures.size(), 0, "Should not have had failures");
        assertEquals(responseCount, expectedMessagesToBeMoved, "Not all messages were moved");
    }
}
