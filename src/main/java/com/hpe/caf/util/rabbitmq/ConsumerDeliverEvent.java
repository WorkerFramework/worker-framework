package com.hpe.caf.util.rabbitmq;


import java.util.Objects;


/**
 * A deliver Event for a Consumer.
 */
public class ConsumerDeliverEvent implements Event<QueueConsumer>
{
    private final Delivery delivery;


    /**
     * Create aa new ConsumerDeliverEvent.
     * @param delivery the combined message with metadata to deliver when this Event is triggered
     */
    public ConsumerDeliverEvent(final Delivery delivery)
    {
        this.delivery = Objects.requireNonNull(delivery);
    }


    /**
     * {@inheritDoc}
     *
     * Hand off the Delivery in this Event to a Consumer for processing.
     */
    @Override
    public void handleEvent(final QueueConsumer consumer)
    {
        consumer.processDelivery(delivery);
    }


    /**
     * @return the Delivery contained by this Event that will be handed off to a Consumer for processing
     */
    public Delivery getDelivery()
    {
        return delivery;
    }
}
