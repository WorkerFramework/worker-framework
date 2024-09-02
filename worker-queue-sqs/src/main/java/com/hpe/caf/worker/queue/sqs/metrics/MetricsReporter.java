/*
 * Copyright 2015-2024 Open Text.
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
package com.hpe.caf.worker.queue.sqs.metrics;

import com.hpe.caf.api.worker.WorkerQueueMetricsReporter;

import java.util.concurrent.atomic.AtomicInteger;

public class MetricsReporter implements WorkerQueueMetricsReporter
{
    private final AtomicInteger queueErrors = new AtomicInteger(0);
    private final AtomicInteger messagesReceived = new AtomicInteger(0);
    private final AtomicInteger messagesPublished = new AtomicInteger(0);
    private final AtomicInteger messagesRejected = new AtomicInteger(0);
    private final AtomicInteger messagesDropped = new AtomicInteger(0);

    public void incrementErrors()
    {
        queueErrors.incrementAndGet();
    }

    public void incrementErrors(final int delta)
    {
        queueErrors.getAndAdd(delta);
    }

    @Override
    public int getQueueErrors()
    {
        return queueErrors.get();
    }

    public void incrementReceived()
    {
        messagesReceived.incrementAndGet();
    }

    public void incrementReceived(final int delta)
    {
        messagesReceived.getAndAdd(delta);
    }

    @Override
    public int getMessagesReceived()
    {
        return messagesReceived.get();
    }

    public void incrementPublished()
    {
        messagesPublished.incrementAndGet();
    }

    public void incrementPublished(final int delta)
    {
        messagesPublished.getAndAdd(delta);
    }

    @Override
    public int getMessagesPublished()
    {
        return messagesPublished.get();
    }

    public void incrementRejected()
    {
        messagesRejected.incrementAndGet();
    }

    @Override
    public int getMessagesRejected()
    {
        return messagesRejected.get();
    }

    public void incrementDropped()
    {
        messagesDropped.incrementAndGet();
    }

    @Override
    public int getMessagesDropped()
    {
        return messagesDropped.get();
    }
}
