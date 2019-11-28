/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
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

import java.util.concurrent.BlockingQueue;

/**
 * Default RabbitPublisher that uses QueuePublisher Event objects. Most implementations that wish to publish basic messages to RabbitMQ
 * should extends this class.
 */
public class DefaultRabbitPublisher extends EventPoller<QueuePublisher>
{
    private static final int POLL_PERIOD = 2;

    /**
     * Create a new DefaultRabbitPublisher
     *
     * @param events the internal queue of events to handle
     * @param pubImpl the implementation of the QueuePublisher
     */
    public DefaultRabbitPublisher(final BlockingQueue<Event<QueuePublisher>> events, final QueuePublisher pubImpl)
    {
        super(POLL_PERIOD, events, pubImpl);
    }
}
