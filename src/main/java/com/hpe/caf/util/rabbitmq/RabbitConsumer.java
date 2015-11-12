package com.hpe.caf.util.rabbitmq;


import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;


/**
 * A basic framework for handling consumption of messages from a RabbitMQ queue.
 * It decouples the RabbitMQ client threads delivering messages from the handling
 * and dispatching of these messages.
 * @since 1.0
 */
public abstract class RabbitConsumer<T> extends EventPoller<T> implements Consumer
{
    private static final Logger LOG = LoggerFactory.getLogger(RabbitConsumer.class);


    /**
     * Create a new RabbitConsumer.
     * @param pollPeriod the polling period to look for events
     * @param events the object to use for storing and polling events
     * @param consumerImpl the event handler implementation
     */
    public RabbitConsumer(int pollPeriod, BlockingQueue<Event<T>> events, T consumerImpl)
    {
        super(pollPeriod, events, consumerImpl);
    }


    @Override
    public final void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
    {
        Map<String, String> headers;
        if ( properties.getHeaders() == null ) {
            headers = Collections.emptyMap();
        } else {
            headers = properties.getHeaders().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> String.class.cast(e.getValue())));
        }
        getEventQueue().add(getDeliverEvent(envelope, body, headers));
    }


    @Override
    public void handleCancel(String consumerTag)
        throws IOException
    {
        LOG.warn("Unexpected channel cancel received for consumer tag {}", consumerTag);
    }


    @Override
    public void handleCancelOk(String consumerTag)
    {
        LOG.debug("Channel cancel received for consumer tag {}", consumerTag);
    }


    @Override
    public void handleConsumeOk(String consumerTag)
    {
        LOG.debug("Channel consuming with consumer tag {}", consumerTag);
    }


    @Override
    public void handleRecoverOk(String consumerTag)
    {
        LOG.info("Channel recovered for consumer tag {}", consumerTag);
    }


    @Override
    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig)
    {
        LOG.warn("Connection was shutdown for consumer tag {}", consumerTag);
    }


    /**
     * Get a new delivery event for internal handling of new messages
     * @param envelope the envelope, containing metadata about the message delivery
     * @param data the actual message delivery
     * @param headers the message headers
     * @return an instance of this implementation's QueueEvent indicating a delivery
     * @since 2.0
     */
    protected abstract Event<T> getDeliverEvent(Envelope envelope, byte[] data, Map<String, String> headers);
}
