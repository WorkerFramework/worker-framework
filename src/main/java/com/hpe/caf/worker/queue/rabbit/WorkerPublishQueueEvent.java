package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.worker.jobtracking.JobTrackingEventType;

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
    private final JobTrackingEventType trackingEventType;
    private final String routingKey;
    private final long tag;
    private final Map<String, String> headerMap;


    /**
     * Create a new WorkerPublishQueueEvent
     * @param messageData the raw message data to publish
     * @param routingKey the routing key to publish the data on
     * @param ackId the id of a message previously consumed to acknowledge
     * @param headers the map of key/value paired headers to be stamped on the message
     * @param trackingEventType the type of tracking update message to be published in addition to the message held in messageData
     */
    public WorkerPublishQueueEvent(byte[] messageData, String routingKey, long ackId, Map<String, String> headers, JobTrackingEventType trackingEventType)
    {
        this.data = Objects.requireNonNull(messageData);
        this.routingKey = Objects.requireNonNull(routingKey);
        this.tag = ackId;
        this.headerMap = Objects.requireNonNull(headers);
        this.trackingEventType = trackingEventType;
    }


    public WorkerPublishQueueEvent(byte[] messageData, String routingKey, long ackId, Map<String, String> headers)
    {
        this(messageData, routingKey, ackId, headers, null);
    }


    public WorkerPublishQueueEvent(byte[] messageData, String routingKey, long ackId)
    {
        this(messageData, routingKey, ackId, Collections.emptyMap());
    }


    @Override
    public void handleEvent(WorkerPublisher target)
    {
        target.handlePublish(data, routingKey, tag, headerMap, trackingEventType);
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


    /**
     * @return the type of tracking update message to be published in addition to the message held in data
     */
    public JobTrackingEventType getTrackingEventType() {
        return trackingEventType;
    }
}
