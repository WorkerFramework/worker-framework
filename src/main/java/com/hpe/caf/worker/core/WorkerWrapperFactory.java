package com.hpe.caf.worker.core;


import com.hpe.caf.api.ServicePath;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerFactory;

import java.util.Objects;


/**
 * Utility class for generating a new WorkerWrapper for an incoming task.
 */
public class WorkerWrapperFactory
{
    private final ServicePath servicePath;
    private final CompleteTaskCallback callback;
    private final WorkerFactory factory;


    /**
     * Create a WorkerWrapperFactory. The constructor parameters are the fixed properties of every WorkerWrapper on
     * this micro-service worker.
     * @param path the service path of this worker service
     * @param callback the callback the wrappers use when a task completes
     * @param workerFactory the origin of the Worker objects themselves
     */
    public WorkerWrapperFactory(final ServicePath path, final CompleteTaskCallback callback, final WorkerFactory workerFactory)
    {
        this.servicePath = Objects.requireNonNull(path);
        this.callback = Objects.requireNonNull(callback);
        this.factory = Objects.requireNonNull(workerFactory);
    }

    /**
     * Get an appropriate WorkerWrapper for the given TaskMessage.
     * @param tm the message to get an appropriately wrapped Worker for
     * @param queueMessageId the queue message ID, used to keep track of this individual message
     * @return a WorkerWrapper for the given TaskMessage
     */
    public WorkerWrapper getWorkerWrapper(final TaskMessage tm, final String queueMessageId)
        throws InvalidTaskException, TaskRejectedException
    {
        return new WorkerWrapper(tm, queueMessageId, getWorker(tm), callback, servicePath);
    }


    private Worker getWorker(final TaskMessage tm)
        throws InvalidTaskException, TaskRejectedException
    {
        byte[] context = tm.getContext().get(servicePath.toString());
        return factory.getWorker(tm.getTaskClassifier(), tm.getTaskApiVersion(), tm.getTaskStatus(), tm.getTaskData(), context);
    }
}
