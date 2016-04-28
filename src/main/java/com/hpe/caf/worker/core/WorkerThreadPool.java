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
     * Pass off a runnable task to the backend, considering a hard upper bound to the internal backlog.
     * @param wrapper the new task to run
     * @param id a unique task id
     * @throws TaskRejectedException if no more tasks can be added to the internal backlog
     */
    void submit(final Runnable wrapper, final String id)
        throws TaskRejectedException;

    int abortTasks();
}
