package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.util.rabbitmq.Event;

import java.util.Objects;


/**
 * Event for publishing via a WorkerPublisher.
 */
public class WorkerPublishQueueEvent implements Event<WorkerPublisher>
{
    private final byte[] data;
    private final String queue;
    private final long tag;


    /**
     * Create a new WorkerPublishQueueEvent
     * @param messageData the raw message data to publish
     * @param messageQueue the name of the queue to publish the data on
     * @param ackId the id of a message previously consumed to acknowledge
     */
    public WorkerPublishQueueEvent(final byte[] messageData, final String messageQueue, final long ackId)
    {
        this.data = Objects.requireNonNull(messageData);
        this.queue = Objects.requireNonNull(messageQueue);
        this.tag = ackId;
    }


    @Override
    public void handleEvent(final WorkerPublisher target)
    {
        target.handlePublish(data, queue, tag);
    }


    public byte[] getData()
    {
        return data;
    }


    public String getQueue()
    {
        return queue;
    }


    public long getTag()
    {
        return tag;
    }
}
