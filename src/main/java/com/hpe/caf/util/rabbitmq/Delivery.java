package com.hpe.caf.util.rabbitmq;


import com.rabbitmq.client.Envelope;

import java.util.Objects;


/**
 * Structure that contains data and metadata from a RabbitMQ queue delivery.
 * @since 1.0
 */
public class Delivery
{
    private final Envelope envelope;
    private final byte[] messageData;


    public Delivery(final Envelope env, final byte[] data)
    {
        this.envelope = Objects.requireNonNull(env);
        this.messageData = data;
    }


    /**
     * @return the envelope containing metadata about the delivery
     */
    public Envelope getEnvelope()
    {
        return envelope;
    }


    /**
     * @return the message delivery itself
     */
    public byte[] getMessageData()
    {
        return messageData;
    }
}
