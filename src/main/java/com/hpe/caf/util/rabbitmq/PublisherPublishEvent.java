package com.hpe.caf.util.rabbitmq;


import java.util.Objects;


/**
 * A publish event for a class implementing the Publisher interface.
 * @since 7.0
 */
public class PublisherPublishEvent implements Event<QueuePublisher>
{
    private final byte[] data;


    /**
     * Create a new PublisherPublishEvent.
     * @param messageData the message data to publish when this Event is triggered
     */
    public PublisherPublishEvent(final byte[] messageData)
    {
        this.data = Objects.requireNonNull(messageData);
    }


    /**
     * {@inheritDoc}
     *
     * Triggers a Publisher to publish the message data contained in this Event.
     */
    @Override
    public void handleEvent(final QueuePublisher target)
    {
        target.handlePublish(data);
    }
}
