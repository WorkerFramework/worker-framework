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
    public void testMessageIsRedeliveredWithSameMessageIdAfterVisibilityTimeoutExpires() throws Exception
    {
        var inputQueue = "expired-visibility";
        final var workerWrapper = getWorkerWrapper(
                inputQueue,
                10,
                1,
                1,
                1000,
                600);
        final var msgBody = "Redelivery";
        sendMessages(workerWrapper, msgBody);

        final var msg = workerWrapper.callbackQueue.poll(10, TimeUnit.SECONDS);
        final var body = msg.body();
        final var messageId = msg.taskInformation().getInboundMessageId();
        Assert.assertEquals(msgBody, body, "Message was not as expected");

        // Let visibility timeout expire
        Thread.sleep(10 * 1000 + 1000);

        final var redeliveredMsg = workerWrapper.callbackQueue.poll(10, TimeUnit.SECONDS);
        purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        final var redeliveredBody = redeliveredMsg.body();
        Assert.assertEquals(
                messageId,
                redeliveredMsg.taskInformation().getInboundMessageId(),
                "Message ids do not match");
        Assert.assertEquals(msgBody, redeliveredBody, "Redelivered message was not as expected");
    }

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

        final var msg = workerWrapper.callbackQueue.poll(3, TimeUnit.SECONDS);
        final var body = msg.body();
        var redeliveredMsg = workerWrapper.callbackQueue.poll(3, TimeUnit.SECONDS);
        try {
            Assert.assertEquals(msgBody, body, "Message was not as expected");
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
                1,
                1,
                1000,
                600);
        final var msgBody = "hello-world";
        sendMessages(workerWrapper, msgBody);

        // Message will normally time out after 10 seconds
        final var msg = workerWrapper.callbackQueue.poll(3, TimeUnit.SECONDS);

        final var sqsTaskInfo = (SQSTaskInformation) msg.taskInformation();

        // Extend timeout to 20 seconds
        workerWrapper.extendTimeout(workerWrapper.inputQueueUrl, 20, sqsTaskInfo);

        // Message should NOT be redelivered @18 seconds later
        var redelivered = workerWrapper.callbackQueue.poll(18, TimeUnit.SECONDS);
        Thread.sleep(3000);
        // Now it should be available
        var redeliveredAgain = workerWrapper.callbackQueue.poll(0, TimeUnit.SECONDS);
        try {
            Assert.assertNull(redelivered, "Message should NOT be delivered @18 seconds later");
            Assert.assertNotNull(redeliveredAgain, "Message should have been redelivered");
        } finally {
            purgeQueue(workerWrapper.sqsClient, workerWrapper.inputQueueUrl);
        }
    }
}
