package com.hp.caf.worker.queue.rabbit;


import com.hp.caf.api.worker.WorkerQueueMetricsReporter;

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
