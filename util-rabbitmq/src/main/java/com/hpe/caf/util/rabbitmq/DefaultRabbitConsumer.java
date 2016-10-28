package com.hpe.caf.util.rabbitmq;


import com.rabbitmq.client.Envelope;

import java.util.Map;
import java.util.concurrent.BlockingQueue;


/**
 * Default RabbitConsumer that uses QueueConsumer Event objects.
 * Most implementations of a RabbitConsumer should extends this class.
 * @since 1.0
 */
public class DefaultRabbitConsumer extends RabbitConsumer<QueueConsumer>
{
    public static final int POLL_PERIOD = 2;


    /**
     * Create a new DefaultRabbitConsumer.
     * @param events the queue of events to handle
     * @param consumer the implementation of the QueueConsumer
     */
    public DefaultRabbitConsumer(BlockingQueue<Event<QueueConsumer>> events, QueueConsumer consumer)
    {
        super(POLL_PERIOD, events, consumer);
    }


    @Override
    protected final Event<QueueConsumer> getDeliverEvent(Envelope envelope, byte[] data, Map<String, Object> headers)
    {
        return new ConsumerDeliverEvent(new Delivery(envelope, data, headers));
    }
}
