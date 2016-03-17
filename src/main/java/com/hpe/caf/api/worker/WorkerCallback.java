package com.hpe.caf.api.worker;

/**
 * The callback interface for a task to report it is complete or that it must
 * be subject to some further action, e.g. forwarding.
 */
public interface WorkerCallback
{
    /**
     * Indicate a task was completed.
     * @param queueMsgId a queue-specific reference for the incoming message that generated the response
     * @param queue the queue to hold the message
     * @param responseMessage the message to put on the queue
     */
    void complete(String queueMsgId, String queue, TaskMessage responseMessage);


    /**
     * Indicates the Worker wishes to abandon this task, but return it to the queue so that it can be
     * retried by this or another Worker instance.
     * @param queueMsgId the id of the task's queue message to reject
     */
    void abandon(String queueMsgId);


    /**
     * Indicates the Worker wishes to forward this task to the specified queue without processing it.
     * @param queueMsgId a queue-specific reference for the incoming message to be forwarded
     * @param queue the queue to hold the forwarded message
     * @param forwardedMessage the message to put on the queue
     */
    void forward(String queueMsgId, String queue, TaskMessage forwardedMessage);


    /**
     * Indicates the Worker wishes to discard this task without returning it to the queue for retry.
     * @param queueMsgId the id of the task's queue message to discard
     */
    void discard(String queueMsgId);
}

