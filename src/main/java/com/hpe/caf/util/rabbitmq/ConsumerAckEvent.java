package com.hpe.caf.util.rabbitmq;


/**
 * An acknowledge Event for a Consumer.
 */
public class ConsumerAckEvent implements Event<QueueConsumer>
{
    private final long tag;


    /**
     * Create a new ConsumerAckEvent
     * @param tag the RabbitMQ id of the message to acknowledge
     */
    public ConsumerAckEvent(final long tag)
    {
        this.tag = tag;
    }


    /**
     * {@inheritDoc}
     *
     * Acknowledges the message in a Consumer.
     */
    @Override
    public void handleEvent(final QueueConsumer target)
    {
        target.processAck(tag);
    }


    /**
     * @return the RabbitMQ id of the message to acknowledge by this Event
     */
    public long getTag()
    {
        return tag;
    }
}
