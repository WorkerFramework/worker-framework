package com.hpe.caf.worker.queue.rabbit;


import java.util.Map;


/**
 * A publisher that publishes to a specific queue and acknowledges another message
 * subsequent to publishing.
 * @since 7.5
 */
public interface WorkerPublisher
{
    /**
     * Publish a new message to a specified queue and acknowledge a prior message by id.
     * @param data the body of the message to publish
     * @param routingKey the routing key to publish the new message with
     * @param ackId the prior message id to acknowledge
     * @param headers key/value map of headers to add to the published message
     * @since 10.6
     */
    void handlePublish(byte[] data, String routingKey, long ackId, Map<String, String> headers);
}
