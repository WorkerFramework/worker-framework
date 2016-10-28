package com.hpe.caf.util.rabbitmq;


/**
 * A message reject Event for a Consumer.
 * @since 1.0
 */
public class ConsumerRejectEvent implements Event<QueueConsumer>
{
    private final long tag;


    /**
     * Create a new ConsumerRejectEvent.
     * @param tag the RabbitMQ id of the message the Consumer should reject when this Event is triggered
     */
    public ConsumerRejectEvent(final long tag)
    {
        this.tag = tag;
    }


    /**
     * {@inheritDoc}
     *
     * Calls a Consumer to drop the message indicated by the id contained within this Event.
     */
    @Override
    public void handleEvent(final QueueConsumer target)
    {
        target.processReject(tag);
    }


    /**
     * @return the RabbitMQ id of the message this Event will trigger a Consumer to reject
     */
    public long getTag()
    {
        return tag;
    }
}
