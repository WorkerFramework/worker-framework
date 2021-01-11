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
 * A publish event for a class implementing the Publisher interface.
 */
public class PublisherPublishEvent implements Event<QueuePublisher>
{
    private final byte[] data;

    /**
     * Create a new PublisherPublishEvent.
     *
     * @param messageData the message data to publish when this Event is triggered
     */
    public PublisherPublishEvent(final byte[] messageData)
    {
        this.data = Objects.requireNonNull(messageData);
    }

    /**
     * {@inheritDoc}
     *
     * Triggers a Publisher to publish the message data contained in this Event.
     */
    @Override
    public void handleEvent(final QueuePublisher target)
    {
        target.handlePublish(data);
    }
}
