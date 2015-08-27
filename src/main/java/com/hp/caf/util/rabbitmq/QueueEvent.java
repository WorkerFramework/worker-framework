package com.hp.caf.util.rabbitmq;


import java.util.Objects;


/**
 * Container for internal events for publishers and consumers.
 * @param <T> the sort of events this QueueEvent can contain
 */
public abstract class QueueEvent<T extends Enum<T>>
{
    private final T eventType;


    public QueueEvent(final T type)
    {
        Objects.requireNonNull(type);
        this.eventType = type;
    }


    /**
     * @return the type of event this is
     */
    public T getEventType()
    {
        return eventType;
    }
}
