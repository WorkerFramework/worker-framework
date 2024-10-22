/*
 * Copyright 2015-2024 Open Text.
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
package com.github.workerframework.worker.queues.rabbit;

import com.github.workerframework.util.rabbitmq.Event;

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
    private final RabbitTaskInformation taskInformation;
    private final Map<String, Object> headerMap;

    /**
     * Create a new WorkerPublishQueueEvent
     *
     * @param messageData the raw message data to publish
     * @param routingKey the routing key to publish the data on
     * @param taskInformation the id of a message previously consumed to acknowledge
     * @param headers the map of key/value paired headers to be stamped on the message
     */
    public WorkerPublishQueueEvent(byte[] messageData, String routingKey, RabbitTaskInformation taskInformation, Map<String, Object> headers)
    {
        this.data = Objects.requireNonNull(messageData);
        this.routingKey = Objects.requireNonNull(routingKey);
        this.taskInformation = taskInformation;
        this.headerMap = Objects.requireNonNull(headers);
    }

    public WorkerPublishQueueEvent(byte[] messageData, String routingKey, RabbitTaskInformation taskInformation)
    {
        this(messageData, routingKey, taskInformation, Collections.emptyMap());
    }

    @Override
    public void handleEvent(WorkerPublisher target)
    {
        target.handlePublish(data, routingKey, taskInformation, headerMap);
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
     * @return the taskInformation containing inbound message id
     */
    public RabbitTaskInformation getTaskInformation()
    {
        return taskInformation;
    }

    /**
     * @return the key/value map of header strings
     */
    public Map<String, Object> getHeaderMap()
    {
        return headerMap;
    }
}
