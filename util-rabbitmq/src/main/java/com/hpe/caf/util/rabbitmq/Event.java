package com.hpe.caf.util.rabbitmq;


/**
 * A general event trigger with a target.
 * @param <T> the class or interface of the target the Event applies to
 * @since 1.0
 */
@FunctionalInterface
public interface Event<T>
{
    /**
     * Trigger the action represented by this Event.
     * @param target the class to perform an action on
     */
    void handleEvent(final T target);
}
