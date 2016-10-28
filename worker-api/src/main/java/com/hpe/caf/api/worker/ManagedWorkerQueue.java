package com.hpe.caf.api.worker;


import com.hpe.caf.api.HealthReporter;


/**
 * A WorkerQueue for use at an application level, supporting management methods.
 */
public interface ManagedWorkerQueue extends HealthReporter, WorkerQueue
{
    /**
     * Open queues to start accepting tasks and results.
     * @param callback the callback to use when registering or aborting tasks
     * @throws QueueException if the queue cannot be started
     */
    void start(TaskCallback callback)
        throws QueueException;


    /**
     * Halt the incoming queue so that no more tasks are picked up.
     */
    void shutdownIncoming();


    /**
     * Terminate all queue operations.
     */
    void shutdown();


    /**
     * @return the metrics implementation for this WorkerQueue
     */
    WorkerQueueMetricsReporter getMetrics();
}
