package com.hpe.caf.worker.core;

import com.hpe.caf.api.worker.TaskRejectedException;
import java.util.concurrent.TimeUnit;

interface WorkerThreadPool {

    static WorkerThreadPool create(final int nThreads) {
        return create(nThreads, () -> System.exit(1));
    }

    static WorkerThreadPool create(final int nThreads, final Runnable handler) {
        return new WorkerThreadPoolImpl(nThreads, handler);
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
