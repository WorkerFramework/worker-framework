package com.hpe.caf.worker.queue.sqs;

import com.hpe.caf.worker.queue.sqs.distributor.SQSMessageDistributor;
import com.hpe.caf.worker.queue.sqs.util.CallbackResponse;
import org.testng.Assert;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.getWorkerWrapper;
import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.purgeQueue;
import static com.hpe.caf.worker.queue.sqs.util.SQSWorkerQueueWrapper.sendMessagesInBatches;

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
        final var workerWrapper = getWorkerWrapper(
                destinationQueue,
                60,
                1,
                10,
                1,
                600);

        var createQueueRequest = CreateQueueRequest.builder()
                .queueName(sourceQueue)
                .build();
        var sourceQueueUrl = workerWrapper.sqsClient.createQueue(createQueueRequest).queueUrl();

        sendMessagesInBatches(workerWrapper.sqsClient, sourceQueueUrl, numberOfMessages);

        var distributor = new SQSMessageDistributor(
                new SQSClientProviderImpl(workerWrapper.sqsConfiguration),
                sourceQueue,
                destinationQueue
        );

        var failures = distributor.moveMessages(numberOfMessagesToMove);

        var receivedMessages = new ArrayList<CallbackResponse>();
        CallbackResponse response;
        do {
            response = workerWrapper.callbackQueue.poll(5, TimeUnit.SECONDS);
            if (response != null) {
                receivedMessages.add(response);
            }
        } while (response != null);

        try {
            Assert.assertEquals(receivedMessages.size(), expectedMessagesToBeMoved, "Not all messages were moved");
            Assert.assertEquals(failures.size(), 0, "Should not have had failures");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
            purgeQueue(workerWrapper.sqsClient, sourceQueueUrl);
        }
    }
}
