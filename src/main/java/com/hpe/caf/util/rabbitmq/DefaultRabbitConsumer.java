package com.hpe.caf.util.rabbitmq;


import com.rabbitmq.client.Envelope;

import java.util.concurrent.BlockingQueue;


/**
 * Default RabbitConsumer that uses QueueConsumer Event objects.
 * Most implementations of a RabbitConsumer should extends this class.
 */
public class DefaultRabbitConsumer extends RabbitConsumer<QueueConsumer>
{
    public static final int POLL_PERIOD = 2;


    /**
     * Create a new DefaultRabbitConsumer.
     * @param events the queue of events to handle
     * @param consumer the implementation of the QueueConsumer
     */
    public DefaultRabbitConsumer(final BlockingQueue<Event<QueueConsumer>> events, final QueueConsumer consumer)
    {
        super(POLL_PERIOD, events, consumer);
    }


    @Override
    protected final Event<QueueConsumer> getDeliverEvent(final Envelope envelope, final byte[] data)
    {
        return new ConsumerDeliverEvent(new Delivery(envelope, data));
    }
}
