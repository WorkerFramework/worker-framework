package com.hpe.caf.util.rabbitmq;


import java.util.concurrent.BlockingQueue;


/**
 * Default RabbitPublisher that uses QueuePublisher Event objects.
 * Most implementations that wish to publish basic messages to RabbitMQ should extends this class.
 */
public class DefaultRabbitPublisher extends EventPoller<QueuePublisher>
{
    public static final int POLL_PERIOD = 2;


    /**
     * Create a new DefaultRabbitPublisher
     * @param events the internal queue of events to handle
     */
    public DefaultRabbitPublisher(final BlockingQueue<Event<QueuePublisher>> events, final QueuePublisher pubImpl)
    {
        super(POLL_PERIOD, events, pubImpl);
    }
}
