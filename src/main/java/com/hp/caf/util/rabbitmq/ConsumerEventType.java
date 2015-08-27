package com.hp.caf.util.rabbitmq;


/**
 * Events that are processed by a consumer.
 */
public enum ConsumerEventType {
    /**
     * A new message has been delivered and is ready to pick up.
     */
    DELIVER,
    /**
     * Indicates an acknowledgement of a previously received message is ready to be sent.
     */
    ACK,
    /**
     * Indicates a rejection (and requeue) of a previously received message.
     */
    REJECT,
    /**
     * Indicates a rejection (*without* requeue) of a previously received message.
     */
    DROP
}
