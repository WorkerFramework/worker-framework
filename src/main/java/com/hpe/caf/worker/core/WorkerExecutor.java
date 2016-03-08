package com.hpe.caf.worker.core;


import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.naming.ServicePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * Utility class for preparing a new Worker for a task and executing it.
 */
public class WorkerExecutor
{
    private final ServicePath servicePath;
    private final WorkerCallback callback;
    private final WorkerFactory factory;
    private final Map<String, Future<?>> tasks;
    private final ThreadPoolExecutor threadPool;
    private static final Logger LOG = LoggerFactory.getLogger(WorkerExecutor.class);


    /**
     * Create a WorkerWrapperFactory. The constructor parameters are the fixed properties of every WorkerWrapper on
     * this micro-service worker.
     * @param path the service path of this worker service
     * @param callback the callback the wrappers use when a task completes
     * @param workerFactory the origin of the Worker objects themselves
     */
    public WorkerExecutor(final ServicePath path, final WorkerCallback callback, final WorkerFactory workerFactory, final Map<String, Future<?>> taskMap,
                          final ThreadPoolExecutor pool)
    {
        this.servicePath = Objects.requireNonNull(path);
        this.callback = Objects.requireNonNull(callback);
        this.factory = Objects.requireNonNull(workerFactory);
        this.tasks = Objects.requireNonNull(taskMap);
        this.threadPool = Objects.requireNonNull(pool);
    }


    /**
     * Get a new Worker for a TaskMessage and hand the Worker off to a thread pool to execute, wrapped in a WorkerWrapper.
     * If the WorkerFactory indicates the task-specific data is invalid, a response is immediately returned indicating this.
     * @param tm the task message
     * @param queueMessageId the reference to the message this task arrived on
     * @throws TaskRejectedException if the WorkerFactory indicates the task cannot be handled at this time
     */
    public void executeTask(final TaskMessage tm, final String queueMessageId)
        throws TaskRejectedException
    {
        try {
            WorkerWrapper wrapper = getWorkerWrapper(tm, queueMessageId);
            submitToThreadPool(wrapper, queueMessageId);
        } catch (InvalidTaskException e) {
            LOG.error("Task data is invalid for {}, returning status {}", tm.getTaskId(), TaskStatus.INVALID_TASK, e);
            TaskMessage invalidResponse =
                new TaskMessage(tm.getTaskId(), tm.getTaskClassifier(), tm.getTaskApiVersion(), new byte[]{}, TaskStatus.INVALID_TASK, tm.getContext());
            callback.complete(queueMessageId, factory.getInvalidTaskQueue(), invalidResponse);
        }
    }


    /**
     * Get a new Worker for a TaskMessage and use it to decide whether the message is to be forwarded or discarded.
     * @param tm the task message
     * @param queueMessageId the reference to the message this task arrived on
     */
    public void forwardTask(final TaskMessage tm, final String queueMessageId) throws TaskRejectedException {
        try {
            Worker worker = getWorker(tm);

            //Check whether the worker can evaluate messages for forwarding.
            if (worker instanceof TaskMessageForwardingEvaluator) {
                ((TaskMessageForwardingEvaluator) worker).determineForwardingAction(tm, queueMessageId, callback);
            } else {
                //Messages are forwarded by default.
                callback.forward(queueMessageId, tm.getTo(), tm);
            }
        } catch (InvalidTaskException e) {
            LOG.error("Task data is invalid for {}, returning status {}", tm.getTaskId(), TaskStatus.INVALID_TASK, e);
            TaskMessage invalidResponse =
                    new TaskMessage(tm.getTaskId(), tm.getTaskClassifier(), tm.getTaskApiVersion(), new byte[]{}, TaskStatus.INVALID_TASK, tm.getContext());
            callback.complete(queueMessageId, factory.getInvalidTaskQueue(), invalidResponse);
        }
    }


    /**
     * Pass off a runnable task to the backend, considering a hard upper bound to the internal backlog.
     * @param wrapper the new task to run
     * @param id a unique task id
     * @throws TaskRejectedException if no more tasks can be added to the internal backlog
     */
    private void submitToThreadPool(final Runnable wrapper, final String id)
        throws TaskRejectedException
    {
        if ( threadPool.getQueue().size() < threadPool.getCorePoolSize() * 10 ) {
            tasks.put(id, threadPool.submit(wrapper));
        } else {
            throw new TaskRejectedException("Maximum internal task backlog exceeded");
        }
    }


    /**
     * Get an appropriate WorkerWrapper for the given TaskMessage.
     * @param tm the message to get an appropriately wrapped Worker for
     * @param queueMessageId the queue message ID, used to keep track of this individual message
     * @return a WorkerWrapper for the given TaskMessage
     */
    private WorkerWrapper getWorkerWrapper(final TaskMessage tm, final String queueMessageId)
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
