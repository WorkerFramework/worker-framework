/*
 * Copyright 2015-2023 Open Text.
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
 * The basic RabbitMQ consumer-side API methods.
 */
public interface QueueConsumer
{
    /**
     * Handle a new message from the RabbitMQ queue
     *
     * @param delivery the newly arrived message including metadata
     */
    void processDelivery(final Delivery delivery);

    /**
     * Acknowledge a message
     *
     * @param tag the RabbitMQ id of the message to acknowledge
     */
    void processAck(final long tag);

    /**
     * Reject a message back onto the queue
     *
     * @param tag the RabbitMQ id of the message to reject
     */
    void processReject(final long tag);

    /**
     * Drop a message
     *
     * @param tag the RabbitMQ id of the message to drop
     */
    void processDrop(final long tag);
}
