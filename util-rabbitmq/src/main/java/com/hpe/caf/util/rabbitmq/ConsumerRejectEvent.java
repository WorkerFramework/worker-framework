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

/**
 * A message reject Event for a Consumer.
 */
public class ConsumerRejectEvent implements Event<QueueConsumer>
{
    private final long tag;

    /**
     * Create a new ConsumerRejectEvent.
     *
     * @param tag the RabbitMQ id of the message the Consumer should reject when this Event is triggered
     */
    public ConsumerRejectEvent(final long tag)
    {
        this.tag = tag;
    }

    /**
     * {@inheritDoc}
     *
     * Calls a Consumer to drop the message indicated by the id contained within this Event.
     */
    @Override
    public void handleEvent(final QueueConsumer target)
    {
        target.processReject(tag);
    }

    /**
     * @return the RabbitMQ id of the message this Event will trigger a Consumer to reject
     */
    public long getTag()
    {
        return tag;
    }
}
