package com.hpe.caf.util.rabbitmq;


/**
 * Possible states of durability for a RabbitMQ queue.
 * @since 6.0
 */
public enum Durability
{
    /**
     * The queue contents are durable and should be disk backed.
     */
    DURABLE,
    /**
     * The queue contents are non-durable.
     */
    NON_DURABLE;
}