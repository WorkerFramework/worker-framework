package com.hpe.caf.util.rabbitmq;


/**
 * The basic RabbitMQ consumer-side API methods.
 * @since 7.0
 */
public interface QueueConsumer
{
    /**
     * Handle a new message from the RabbitMQ queue
     * @param delivery the newly arrived message including metadata
     */
    void processDelivery(final Delivery delivery);


    /**
     * Acknowledge a message
     * @param tag the RabbitMQ id of the message to acknowledge
     */
    void processAck(final long tag);


    /**
     * Reject a message back onto the queue
     * @param tag the RabbitMQ id of the message to reject
     */
    void processReject(final long tag);


    /**
     * Drop a message
     * @param tag the RabbitMQ id of the message to drop
     */
    void processDrop(final long tag);
}
