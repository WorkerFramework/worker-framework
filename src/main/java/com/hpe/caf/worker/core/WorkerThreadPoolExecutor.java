package com.hpe.caf.worker.core;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * ThreadPoolExecutor to perform specific actions if a thread in the pool terminate abnormally.
 */
class WorkerThreadPoolExecutor extends ThreadPoolExecutor
{
    private final Procedure throwableHandler;
    private static final Logger LOG = LoggerFactory.getLogger(WorkerThreadPoolExecutor.class);


    public WorkerThreadPoolExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit,
                                    final BlockingQueue<Runnable> workQueue, final Procedure handler)
    {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.throwableHandler = Objects.requireNonNull(handler);
    }


    @Override
    public void afterExecute(final Runnable r, final Throwable t)
    {
        super.afterExecute(r, t);
        if ( t != null ) {
            LOG.error("Worker thread terminated with unhandled throwable, terminating service", t);
            throwableHandler.execute();
        }
    }
}
