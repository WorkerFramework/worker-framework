package com.hpe.caf.api.worker;


import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationSource;


/**
 * Boilerplate for retrieving a WorkerQueue implementation.
 * @since 9.0
 */
public interface WorkerQueueProvider
{
    /**
     * Create a new WorkerQueue instance.
     * @param configurationSource used for configuring the WorkerQueue
     * @param maxTasks the maximum number of tasks the worker can perform at once
     * @param codec the Codec that can be used to serialise/deserialise data
     * @return a new WorkerQueue instance
     * @throws QueueException if a WorkerQueue could not be created
     */
    ManagedWorkerQueue getWorkerQueue(ConfigurationSource configurationSource, int maxTasks, Codec codec)
        throws QueueException;
}
