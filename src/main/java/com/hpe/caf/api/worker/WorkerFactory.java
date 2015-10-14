package com.hpe.caf.api.worker;


import com.hpe.caf.api.HealthReporter;


/**
 * Instantiates a new instance of a Worker given task-specific data.
 * Most implementations of WorkerFactory should extend DefaultWorkerFactory
 * @since 4.0
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
     * @throws TaskRejectedException if a Worker cannot be created to handle this task currently
     * @throws InvalidTaskException if it appears this task cannot possibly be handled by a Worker of this type
     * @since 6.0
     */
    public abstract Worker getWorker(final String classifier, final int version, final TaskStatus status,
                                     final byte[] data, final byte[] context)
        throws TaskRejectedException, InvalidTaskException;


    /**
     * @return the queue to put responses to invalid tasks upon, may be the same as the Worker's result queue
     * @since 8.0
     */
    public abstract String getInvalidTaskQueue();
}
