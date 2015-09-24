package com.hpe.caf.util.rabbitmq;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;


/**
 * A more complicated Event whereby a Future is obtainable in order to block/wait upon the result of it.
 * @param <T> the type of the Event
 * @param <V> the type of the returned value from the Future
 * @since 8.0
 */
public abstract class FutureEvent<T,V> implements Event<T>
{
    private final CompletableFuture<V> future = new CompletableFuture<>();
    private static final Logger LOG = LoggerFactory.getLogger(FutureEvent.class);


    /**
     * @return the Future object to interrogate the result of the Event
     */
    public CompletableFuture<V> ask()
    {
        return future;
    }


    /**
     * {@inheritDoc}
     *
     * The Event will be marked as complete with the value from getEventResult(T). This can be interrogated
     * with the CompletableFuture accessible via ask(). Any exceptions will also trigger completion of the
     * Future with completeExceptionally.
     */
    @Override
    public final void handleEvent(final T target)
    {
        try {
            future.complete(getEventResult(target));
        } catch (Exception e) {
            LOG.warn("Propagating exception from FutureEvent {}", getClass().getSimpleName(), e);
            future.completeExceptionally(e);
        }
    }


    /**
     * Trigger the event and return a result.
     * @param target the class to perform an action on
     * @return a result from the triggering of the Event
     */
    protected abstract V getEventResult(final T target)
        throws Exception;
}
