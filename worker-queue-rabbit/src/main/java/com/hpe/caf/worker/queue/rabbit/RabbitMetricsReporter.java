/*
 * (c) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.api.worker.WorkerQueueMetricsReporter;

import java.util.concurrent.atomic.AtomicInteger;


public class RabbitMetricsReporter implements WorkerQueueMetricsReporter
{
    private final AtomicInteger queueErrors = new AtomicInteger(0);
    private final AtomicInteger msgRx = new AtomicInteger(0);
    private final AtomicInteger msgTx = new AtomicInteger(0);
    private final AtomicInteger msgRejected = new AtomicInteger(0);
    private final AtomicInteger msgDropped = new AtomicInteger(0);


    public void incremementErrors()
    {
        queueErrors.incrementAndGet();
    }


    @Override
    public int getQueueErrors()
    {
        return queueErrors.get();
    }


    public void incrementReceived()
    {
        msgRx.incrementAndGet();
    }


    @Override
    public int getMessagesReceived()
    {
        return msgRx.get();
    }


    public void incrementPublished()
    {
        msgTx.incrementAndGet();
    }


    @Override
    public int getMessagesPublished()
    {
        return msgTx.get();
    }


    public void incrementRejected()
    {
        msgRejected.incrementAndGet();
    }


    @Override
    public int getMessagesRejected()
    {
        return msgRejected.get();
    }


    public void incrementDropped()
    {
        msgDropped.incrementAndGet();
    }


    @Override
    public int getMessagesDropped()
    {
        return msgDropped.get();
    }
}
