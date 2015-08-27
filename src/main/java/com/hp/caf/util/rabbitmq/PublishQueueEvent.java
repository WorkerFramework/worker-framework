package com.hp.caf.util.rabbitmq;


import java.util.Objects;


/**
 * A unit of structured data to be handled by a RabbitPublisher.
 */
public final class PublishQueueEvent extends QueueEvent<PublishEventType>
{
    private final long messageTag;
    private byte[] eventData;
    private String queue;


    public PublishQueueEvent(final PublishEventType type, final long tag, final byte[] data, final String queue)
    {
        super(type);
        this.messageTag = tag;
        this.eventData = Objects.requireNonNull(data);
        this.queue = Objects.requireNonNull(queue);
    }


    /**
     * @return the rabbit message tag
     */
    public long getMessageTag()
    {
        return messageTag;
    }


    /**
     * @return the data associated with this event message
     */
    public byte[] getEventData()
    {
        return eventData;
    }


    /**
     * @return the queue this message is targetted for
     */
    public String getQueue()
    {
        return queue;
    }
}
