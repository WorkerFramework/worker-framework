package com.hpe.caf.util.rabbitmq;


import com.rabbitmq.client.Envelope;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;


/**
 * Structure that contains data and metadata from a RabbitMQ queue delivery.
 * @since 1.0
 */
public class Delivery
{
    private final Envelope envelope;
    private final byte[] messageData;
    private final Map<String, String> headers;


    /**
     * Create a new Delivery, with specific headers.
     * @param env the RabbitMQ message envelope
     * @param data the RabbitMQ message body
     * @param headers the string-mapped key/value headers
     * @since 2.0
     */
    public Delivery(Envelope env, byte[] data, Map<String, String> headers)
    {
        this.envelope = Objects.requireNonNull(env);
        this.messageData = Objects.requireNonNull(data);
        this.headers = Objects.requireNonNull(headers);
    }


    /**
     * Create a new Delivery without headers.
     * @param env the RabbitMQ message envelope
     * @param data the RabbitMQ message body
     */
    public Delivery(Envelope env, byte[] data)
    {
        this(env, data, Collections.emptyMap());
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


    /**
     * @return headers for the message
     * @since 2.0
     */
    public Map<String, String> getHeaders()
    {
        return headers;
    }
}
