package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.util.rabbitmq.Event;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;


/**
 * Event for publishing via a WorkerPublisher.
 * @since 7.5
 */
public class WorkerPublishQueueEvent implements Event<WorkerPublisher>
{
    private final byte[] data;
    private final String routingKey;
    private final long tag;
    private final Map<String, String> headerMap;


    /**
     * Create a new WorkerPublishQueueEvent
     * @param messageData the raw message data to publish
     * @param routingKey the routing key to publish the data on
     * @param ackId the id of a message previously consumed to acknowledge
     * @param headers the map of key/value paired headers to be stamped on the message
     * @since 10.6
     */
    public WorkerPublishQueueEvent(byte[] messageData, String routingKey, long ackId, Map<String, String> headers)
    {
        this.data = Objects.requireNonNull(messageData);
        this.routingKey = Objects.requireNonNull(routingKey);
        this.tag = ackId;
        this.headerMap = Objects.requireNonNull(headers);
    }


    public WorkerPublishQueueEvent(byte[] messageData, String routingKey, long ackId)
    {
        this(messageData, routingKey, ackId, Collections.emptyMap());
    }


    @Override
    public void handleEvent(WorkerPublisher target)
    {
        target.handlePublish(data, routingKey, tag, headerMap);
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
     * @since 10.6
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
     * @since 10.6
     */
    public Map<String, String> getHeaderMap()
    {
        return headerMap;
    }
}
