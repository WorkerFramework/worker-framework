/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * A basic framework for handling consumption of messages from a RabbitMQ queue. It decouples the RabbitMQ client threads delivering
 * messages from the handling and dispatching of these messages.
 */
public abstract class RabbitConsumer<T> extends EventPoller<T> implements Consumer
{
    private static final Logger LOG = LoggerFactory.getLogger(RabbitConsumer.class);

    /**
     * Create a new RabbitConsumer.
     *
     * @param pollPeriod the polling period to look for events
     * @param events the object to use for storing and polling events
     * @param consumerImpl the event handler implementation
     */
    public RabbitConsumer(int pollPeriod, BlockingQueue<Event<T>> events, T consumerImpl)
    {
        super(pollPeriod, events, consumerImpl);
    }

    @Override
    public final void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
    {
        getEventQueue().add(getDeliverEvent(envelope, body, properties.getHeaders() == null ? Collections.emptyMap() : properties.getHeaders()));
    }

    @Override
    public void handleCancel(String consumerTag)
        throws IOException
    {
        LOG.warn("Unexpected channel cancel received for consumer tag {}", consumerTag);
    }

    @Override
    public void handleCancelOk(String consumerTag)
    {
        LOG.debug("Channel cancel received for consumer tag {}", consumerTag);
    }

    @Override
    public void handleConsumeOk(String consumerTag)
    {
        LOG.debug("Channel consuming with consumer tag {}", consumerTag);
    }

    @Override
    public void handleRecoverOk(String consumerTag)
    {
        LOG.info("Channel recovered for consumer tag {}", consumerTag);
    }

    @Override
    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig)
    {
        LOG.warn("Connection was shutdown for consumer tag {}", consumerTag);
    }

    /**
     * Get a new delivery event for internal handling of new messages
     *
     * @param envelope the envelope, containing metadata about the message delivery
     * @param data the actual message delivery
     * @param headers the message headers
     * @return an instance of this implementation's QueueEvent indicating a delivery
     */
    protected abstract Event<T> getDeliverEvent(Envelope envelope, byte[] data, Map<String, Object> headers);
}
