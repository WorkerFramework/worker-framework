package com.hp.caf.util.rabbitmq;


/**
 * Possible states of durability for a RabbitMQ queue.
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