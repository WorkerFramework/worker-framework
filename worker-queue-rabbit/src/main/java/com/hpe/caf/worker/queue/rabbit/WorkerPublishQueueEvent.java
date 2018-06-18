/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.queue.rabbit;

import com.hpe.caf.util.rabbitmq.Event;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Event for publishing via a WorkerPublisher.
 */
public class WorkerPublishQueueEvent implements Event<WorkerPublisher>
{
    private final byte[] data;
    private final String routingKey;
    private final long tag;
    private final Map<String, Object> headerMap;
    private final int priority;

    /**
     * Create a new WorkerPublishQueueEvent
     *
     * @param messageData the raw message data to publish
     * @param routingKey the routing key to publish the data on
     * @param ackId the id of a message previously consumed to acknowledge
     * @param headers the map of key/value paired headers to be stamped on the message
     */
    public WorkerPublishQueueEvent(byte[] messageData, String routingKey, long ackId, Map<String, Object> headers)
    {
        this(messageData, routingKey, ackId, headers, 0);
    }

    /**
     * Create a new WorkerPublishQueueEvent
     *
     * @param messageData the raw message data to publish
     * @param routingKey the routing key to publish the data on
     * @param ackId the id of a message previously consumed to acknowledge
     * @param headers the map of key/value paired headers to be stamped on the message
     */
    public WorkerPublishQueueEvent(byte[] messageData, String routingKey, long ackId, Map<String, Object> headers, int priority)
    {
        this.data = Objects.requireNonNull(messageData);
        this.routingKey = Objects.requireNonNull(routingKey);
        this.tag = ackId;
        this.headerMap = Objects.requireNonNull(headers);
        this.priority = priority;
    }

    public WorkerPublishQueueEvent(byte[] messageData, String routingKey, long ackId)
    {
        this(messageData, routingKey, ackId, Collections.emptyMap());
    }

    @Override
    public void handleEvent(WorkerPublisher target)
    {
        target.handlePublish(data, routingKey, tag, headerMap, priority);
    }

    /**
     * @return the raw data to publish
     */
    public byte[] getData()
    {
        return data;
    }

    /**
     * @return the routing key that will be used to publish this message with
     */
    public String getRoutingKey()
    {
        return routingKey;
    }

    /**
     * @return the rabbitmq message tag to acknowledge upon publishing
     */
    public long getTag()
    {
        return tag;
    }

    /**
     * @return the key/value map of header strings
     */
    public Map<String, Object> getHeaderMap()
    {
        return headerMap;
    }
}
