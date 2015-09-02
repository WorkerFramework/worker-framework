package com.hpe.caf.util.rabbitmq;


import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A basic framework for handling consumption of messages from a RabbitMQ queue.
 * It will listen for ConsumerQueueEvent objects appearing on the consumerEvents
 * queue, and delegate them for processing. The handleDelivery() method of the
 * base QueueingConsumer class is overridden to internally register delivery events.
 */
public abstract class RabbitConsumer<T extends QueueEvent<ConsumerEventType>> extends QueueingConsumer implements Runnable
{
    /**
     * All events for the Thread to handle.
     */
    private final BlockingQueue<T> consumerEvents;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private static final Logger LOG = LoggerFactory.getLogger(RabbitConsumer.class);


    public RabbitConsumer(final BlockingQueue<Delivery> q, final BlockingQueue<T> events, final Channel channel)
    {
        super(channel, q);
        this.consumerEvents = Objects.requireNonNull(events);
    }


    /**
     * Poll the consumerEvents queue for events, and then delegate them to implementation
     * specific processing.
     */
    @Override
    public final void run()
    {
        while ( running.get() ) {
            try {
                T event = consumerEvents.poll(2, TimeUnit.SECONDS);
                if ( event != null ) {
                    switch (event.getEventType()) {
                        case DELIVER:
                            processDelivery(event);
                            break;
                        case ACK:
                            processAck(event);
                            break;
                        case REJECT:
                            processReject(event);
                            break;
                        case DROP:
                            processDrop(event);
                            break;
                        default:
                            LOG.warn("Unknown consumer event received, ignoring");
                            break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        LOG.debug("Terminating");
    }


    /**
     * {@inheritDoc}
     *
     * Delegate internal message delivery to the superclass, but register the arrival of a new message
     * by adding a DELIVER ConsumerQueueEvent to the consumerEvents queue.
     */
    @Override
    public final void handleDelivery(final String consumerTag, final Envelope envelope, final AMQP.BasicProperties properties, final byte[] body)
        throws IOException
    {
        super.handleDelivery(consumerTag, envelope, properties, body);
        consumerEvents.add(getDeliverEvent(envelope.getDeliveryTag()));
    }


    /**
     * Signal the termination of the RabbitConsumer.
     */
    public final void shutdown()
    {
        running.set(false);
    }


    /**
     * Get a new delivery event for internal handling of new messages
     * @param tag the tag of the newly arrived message
     * @return an instance of this implementation's QueueEvent indicating a delivery
     */
    protected abstract T getDeliverEvent(final long tag);


    /**
     * @return access to the consumerEvents queue
     */
    protected final BlockingQueue<T> getConsumerEvents()
    {
        return this.consumerEvents;
    }


    /**
     * Handle a new message from the RabbitMQ queue
     * @param delivery the new message with envelope
     * @param event the event that triggered this delivery
     */
    protected abstract void processDelivery(final Delivery delivery, final T event);


    /**
     * Acknowledge a message
     * @param event the event that triggered this message acknowledgement
     */
    protected abstract void processAck(final T event);


    /**
     * Reject a message back onto the queue
     * @param event the event that triggered this message rejection
     */
    protected abstract void processReject(final T event);


    /**
     * Drop a message
     * @param event the event that triggered this message drop
     */
    protected abstract void processDrop(final T event);


    /**
     * Bridge to get a Delivery from the superclass and delegate it to the subclass.
     * @throws InterruptedException if polling the delivery queue is interrupted
     */
    private void processDelivery(final T event)
        throws InterruptedException
    {
        processDelivery(super.nextDelivery(), event);
    }
}
