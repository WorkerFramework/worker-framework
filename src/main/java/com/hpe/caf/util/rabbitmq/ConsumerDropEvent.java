package com.hpe.caf.util.rabbitmq;


/**
 * A message drop Event for a Consumer.
 */
public class ConsumerDropEvent implements Event<QueueConsumer>
{
    private final long tag;


    /**
     * Create a ConsumerDropEvent.
     * @param tag the RabbitMQ id of the message to drop
     */
    public ConsumerDropEvent(final long tag)
    {
        this.tag = tag;
    }


    /**
     * {@inheritDoc}
     *
     * Triggers a Consumer to drop the message indicated by this Event.
     */
    @Override
    public void handleEvent(final QueueConsumer target)
    {
        target.processDrop(tag);
    }


    /**
     * @return the RabbitMQ id of the message this Event will trigger a Consumer to drop
     */
    public long getTag()
    {
        return tag;
    }
}
