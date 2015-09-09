package com.hpe.caf.worker.queue.rabbit;


/**
 * A publisher that publishes to a specific queue and acknowledges another message
 * subsequent to publishing.
 */
public interface WorkerPublisher
{
    /**
     * Publish a new message to a specified queue and acknowledge a prior message by id.
     * @param data the body of the message to publish
     * @param queueName the name of the queue to publish the new message to
     * @param ackId the prior message id to acknowledge
     */
    void handlePublish(final byte[] data, final String queueName, final long ackId);
}
