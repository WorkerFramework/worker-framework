package com.hpe.caf.util.rabbitmq;


import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A simple publisher handler, that will wait for PublishQueueEvent objects appearing on the
 * publishEvents queue and delegate them for processing.
 */
public abstract class RabbitPublisher<T extends QueueEvent<PublishEventType>> implements Runnable
{
    private final BlockingQueue<T> publishEvents;
    private final Channel channel;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private static final Logger LOG = LoggerFactory.getLogger(RabbitPublisher.class);


    public RabbitPublisher(final BlockingQueue<T> events, final Channel channel)
    {
        this.publishEvents = Objects.requireNonNull(events);
        this.channel = Objects.requireNonNull(channel);
    }


    /**
     * This thread will loop until shutdown() is called, polling for events and passing them
     * off to the appropriate handler.
     */
    @Override
    public final void run()
    {
        while ( running.get() ) {
            try {
                T event = publishEvents.poll(2, TimeUnit.SECONDS);
                if ( event != null && event.getEventType() == PublishEventType.PUBLISH ) {
                    handlePublish(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        LOG.debug("Terminating");
    }


    /**
     * Terminate the thread at the next opportunity. No more events will be handled.
     */
    public final void shutdown()
    {
        running.set(false);
    }


    /**
     * Deal with a new publish event.
     * @param event the event that triggered this call
     */
    protected abstract void handlePublish(final T event);


    /**
     * @return the RabbitMQ channel associated with this publisher
     */
    protected final Channel getChannel()
    {
        return this.channel;
    }
}
