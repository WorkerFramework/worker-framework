package com.hp.caf.util.rabbitmq;


/**
 * A unit of structured data to be handled by a RabbitConsumer.
 */
public final class ConsumerQueueEvent extends QueueEvent<ConsumerEventType>
{
    private final long messageTag;


    public ConsumerQueueEvent(final ConsumerEventType type, final long tag)
    {
        super(type);
        this.messageTag = tag;
    }


    /**
     * @return the rabbit message tag
     */
    public long getMessageTag()
    {
        return messageTag;
    }
}
