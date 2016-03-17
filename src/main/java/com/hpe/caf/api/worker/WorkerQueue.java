package com.hpe.caf.api.worker;


/**
 * A general representation of a queue for the purposes of a worker service.
 * @since 9.0
 */
public interface WorkerQueue
{
    /**
     * Acknowledge the original received message but send out a new message to a target queue.
     * @param acknowledgeId the internal queue message id of the message to acknowledge
     * @param taskMessage the message to publish
     * @param targetQueue the queue to put the message upon
     * @throws QueueException if the message cannot be submitted
     */
    void publish(String acknowledgeId, byte[] taskMessage, String targetQueue)
        throws QueueException;


    /**
     * Called from the asynchronous worker service to notify the queue that it is rejecting a task.
     * It is up to the queue implementation as to whether submit this task to retry or not.
     * @param messageId the queue task id that has been rejected
     */
    void rejectTask(String messageId);


    /**
     * Called from the asynchronous worker service to notify the queue that it is discarding a task.
     * @param messageId the queue task id that has been discarded
     */
    void discardTask(String messageId);


    /**
     * Return the name of the input queue.
     * @return the name of the input queue
     */
    String getInputQueue();
}
