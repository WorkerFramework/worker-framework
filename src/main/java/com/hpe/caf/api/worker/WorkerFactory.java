package com.hpe.caf.api.worker;


import com.hpe.caf.api.HealthReporter;


/**
 * Instantiates a new instance of a Worker given task-specific data.
 */
public abstract class WorkerFactory implements HealthReporter
{
    /**
     * Instantiate a new worker for given task data
     * @param classifier the classifier indicating the type of message it is
     * @param version the api version of the task's message
     * @param status the status of the task
     * @param data the raw serialised task data
     * @param context provides access to task specific context, may be null
     * @return a new Worker instance that will perform work upon the taskData
     * @throws WorkerException if a new Worker could not be created
     */
    public abstract Worker getWorker(final String classifier, final int version, final TaskStatus status,
                                     final byte[] data, final byte[] context)
        throws WorkerException;
}
