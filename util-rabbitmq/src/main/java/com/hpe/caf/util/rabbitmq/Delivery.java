/*
 * Copyright 2018-2017 EntIT Software LLC, a Micro Focus company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.util.rabbitmq;

import com.rabbitmq.client.Envelope;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Structure that contains data and metadata from a RabbitMQ queue delivery.
 */
public class Delivery
{
    private final Envelope envelope;
    private final byte[] messageData;
    private final Map<String, Object> headers;

    /**
     * Create a new Delivery, with specific headers.
     *
     * @param env the RabbitMQ message envelope
     * @param data the RabbitMQ message body
     * @param headers the string-mapped key/value headers
     */
    public Delivery(Envelope env, byte[] data, Map<String, Object> headers)
    {
        this.envelope = Objects.requireNonNull(env);
        this.messageData = Objects.requireNonNull(data);
        this.headers = Objects.requireNonNull(headers);
    }

    /**
     * Create a new Delivery without headers.
     *
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
     */
    public Map<String, Object> getHeaders()
    {
        return headers;
    }
}
