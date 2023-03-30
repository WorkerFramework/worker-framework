/*
 * Copyright 2015-2023 Open Text.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.util.rabbitmq;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An abstract class to poll a BlockingQueue for events and defer them for handling.
 *
 * @param <T> the sort of Event this EventPoller will use
 */
public class EventPoller<T> implements Runnable
{
    private final int pollPeriod;
    /**
     * All events for the Thread to handle.
     */
    private final BlockingQueue<Event<T>> eventQueue;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final T eventHandler;

    /**
     * Create a new EventPoller.
     *
     * @param pollPeriod the period in which to poll the event queue, which will also affect the shutdown time
     * @param eventQueue the object to use for storing and polling events
     * @param eventHandler the implementation to handle events
     */
    public EventPoller(final int pollPeriod, final BlockingQueue<Event<T>> eventQueue, final T eventHandler)
    {
        this.eventQueue = Objects.requireNonNull(eventQueue);
        this.pollPeriod = pollPeriod;
        this.eventHandler = Objects.requireNonNull(eventHandler);
    }

    /**
     * Start a thread that will poll with the period specified when the object was created. Each event received will be handed off to the
     * event handler which inheriting classes specify. This thread will properly query and re-raise the interrupt flag.
     */
    @Override
    public void run()
    {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Event<T> event = eventQueue.poll(pollPeriod, TimeUnit.SECONDS);
                if (event != null) {
                    event.handleEvent(eventHandler);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * @return the internal event queue
     */
    protected final BlockingQueue<Event<T>> getEventQueue()
    {
        return eventQueue;
    }

    /**
     * Signal the termination of the EventPoller. It will terminate at the next possible opportunity. If the EventPoller is not running
     * this has no effect.
     */
    public final void shutdown()
    {
        running.set(false);
    }
}
