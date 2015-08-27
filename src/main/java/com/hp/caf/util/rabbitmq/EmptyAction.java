package com.hp.caf.util.rabbitmq;


/**
 * Possible actions to perform when a queue is empty for a RabbitMQ queue.
 */
public enum EmptyAction
{
    /**
     * Automatically remove this queue when it is empty.
     */
    AUTO_REMOVE,
    /**
     * Do not automatically remove the queue if it becomes empty.
     */
    LEAVE_EMPTY
}