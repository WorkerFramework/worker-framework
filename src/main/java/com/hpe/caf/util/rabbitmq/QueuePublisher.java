package com.hpe.caf.util.rabbitmq;


/**
 * Simplest possible RabbitMQ publishing API
 * @since 7.0
 */
public interface QueuePublisher
{
    /**
     * Publish a new message
     * @param data the raw content of the message to publish
     */
    void handlePublish(final byte[] data);
}
