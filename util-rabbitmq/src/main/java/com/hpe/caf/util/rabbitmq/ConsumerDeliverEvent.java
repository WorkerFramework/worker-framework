/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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

import java.util.Objects;

/**
 * A deliver Event for a Consumer.
 */
public class ConsumerDeliverEvent implements Event<QueueConsumer>
{
    private final Delivery delivery;

    /**
     * Create aa new ConsumerDeliverEvent.
     *
     * @param delivery the combined message with metadata to deliver when this Event is triggered
     */
    public ConsumerDeliverEvent(final Delivery delivery)
    {
        this.delivery = Objects.requireNonNull(delivery);
    }

    /**
     * {@inheritDoc}
     *
     * Hand off the Delivery in this Event to a Consumer for processing.
     */
    @Override
    public void handleEvent(final QueueConsumer consumer)
    {
        consumer.processDelivery(delivery);
    }

    /**
     * @return the Delivery contained by this Event that will be handed off to a Consumer for processing
     */
    public Delivery getDelivery()
    {
        return delivery;
    }
}
