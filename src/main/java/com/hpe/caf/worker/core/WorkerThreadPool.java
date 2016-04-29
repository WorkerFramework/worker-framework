package com.hpe.caf.worker.core;

import com.hpe.caf.api.worker.BulkWorker;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.WorkerFactory;
import java.util.concurrent.TimeUnit;

interface WorkerThreadPool
{
    /* private */ static final Runnable defaultHandler = () -> System.exit(1);

    static WorkerThreadPool create(final int nThreads) {
        return create(nThreads, defaultHandler);
    }

    static WorkerThreadPool create(final WorkerFactory workerFactory) {
        return create(workerFactory, defaultHandler);
    }

    static WorkerThreadPool create(final WorkerFactory workerFactory, final Runnable handler) {
        if (workerFactory instanceof BulkWorker) {
            return new BulkWorkerThreadPool(workerFactory, handler);
        }
        else {
            return create(workerFactory.getWorkerThreads(), handler);
        }
    }

    static WorkerThreadPool create(final int nThreads, final Runnable handler) {
        return new StreamingWorkerThreadPool(nThreads, handler);
    }

    void shutdown();

    void awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Returns whether or not any threads are active
     * @return true if there are no active threads
     */
    boolean isIdle();

    int getBacklogSize();

    /**
     * Execute the specified task at some point in the future
     * @param workerTask the task to be run
     * @throws TaskRejectedException if no more tasks can be accepted
     */
    void submitWorkerTask(WorkerTaskImpl workerTask)
        throws TaskRejectedException;

    int abortTasks();
}
