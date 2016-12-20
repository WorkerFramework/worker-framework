/*
 * (c) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hpe.caf.util.rabbitmq;


import com.rabbitmq.client.Envelope;

import java.util.Map;
import java.util.concurrent.BlockingQueue;


/**
 * Default RabbitConsumer that uses QueueConsumer Event objects.
 * Most implementations of a RabbitConsumer should extends this class.
 */
public class DefaultRabbitConsumer extends RabbitConsumer<QueueConsumer>
{
    public static final int POLL_PERIOD = 2;


    /**
     * Create a new DefaultRabbitConsumer.
     * @param events the queue of events to handle
     * @param consumer the implementation of the QueueConsumer
     */
    public DefaultRabbitConsumer(BlockingQueue<Event<QueueConsumer>> events, QueueConsumer consumer)
    {
        super(POLL_PERIOD, events, consumer);
    }


    @Override
    protected final Event<QueueConsumer> getDeliverEvent(Envelope envelope, byte[] data, Map<String, Object> headers)
    {
        return new ConsumerDeliverEvent(new Delivery(envelope, data, headers));
    }
}
