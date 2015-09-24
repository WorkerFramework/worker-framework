package com.hpe.caf.util.rabbitmq;


import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;


/**
 * A basic framework for handling consumption of messages from a RabbitMQ queue.
 * It decouples the RabbitMQ client threads delivering messages from the handling
 * and dispatching of these messages, which is what this class does.
 * @since 6.0
 */
public abstract class RabbitConsumer<T> extends EventPoller<T> implements Consumer
{
    private static final Logger LOG = LoggerFactory.getLogger(RabbitConsumer.class);


    /**
     * Create a new RabbitConsumer.
     * @param pollPeriod the polling period to look for events
     * @param events the object to use for storing and polling events
     * @param consumerImpl the event handler implementation
     * @since 7.0
     */
    public RabbitConsumer(final int pollPeriod, final BlockingQueue<Event<T>> events, final T consumerImpl)
    {
        super(pollPeriod, events, consumerImpl);
    }


    /**
     * {@inheritDoc}
     *
     * Delegate internal message delivery to the superclass, but register the arrival of a new message
     * by adding a DELIVER ConsumerQueueEvent to the consumerEvents queue.
     * @since 7.0
     */
    @Override
    public final void handleDelivery(final String consumerTag, final Envelope envelope, final AMQP.BasicProperties properties, final byte[] body)
    {
        getEventQueue().add(getDeliverEvent(envelope, body));
    }


    /** @since 7.0 **/
    @Override
    public void handleCancel(final String consumerTag)
        throws IOException
    {
        LOG.warn("Unexpected channel cancel received for consumer tag {}", consumerTag);
    }


    /** @since 7.0 **/
    @Override
    public void handleCancelOk(final String consumerTag)
    {
        LOG.debug("Channel cancel received for consumer tag {}", consumerTag);
    }


    /** @since 7.0 **/
    @Override
    public void handleConsumeOk(final String consumerTag)
    {
        LOG.debug("Channel consuming with consumer tag {}", consumerTag);
    }


    /** @since 7.0 **/
    @Override
    public void handleRecoverOk(final String consumerTag)
    {
        LOG.info("Channel recovered for consumer tag {}", consumerTag);
    }


    /** @since 7.0 **/
    @Override
    public void handleShutdownSignal(final String consumerTag, final ShutdownSignalException sig)
    {
        LOG.warn("Connection was shutdown for consumer tag {}", consumerTag);
    }


    /**
     * Get a new delivery event for internal handling of new messages
     * @param envelope the envelope, containing metadata about the message delivery
     * @param data the actual message delivery
     * @return an instance of this implementation's QueueEvent indicating a delivery
     * @since 7.0
     */
    protected abstract Event<T> getDeliverEvent(final Envelope envelope, final byte[] data);
}
