package com.hpe.caf.worker.core;


import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.DecodeMethod;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerQueue;
import com.hpe.caf.naming.ServicePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * WorkerCore represents the main logic of the microservice worker. It is responsible for accepting
 * new tasks from a WorkerQueue, handing them off to a backend Worker and executing them upon a thread pool.
 * It will then accept a result from the Worker it executed and hand the TaskResult back to the WorkerQueue
 * for publishing.
 */
public class WorkerCore
{
    private final ThreadPoolExecutor threadPool;
    private final ManagedWorkerQueue workerQueue;
    private final WorkerStats stats = new WorkerStats();
    private final TaskCallback callback;
    private final ConcurrentMap<String, Future<?>> tasks = new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(WorkerCore.class);


    public WorkerCore(final Codec codec, final ThreadPoolExecutor pool, final ManagedWorkerQueue queue, final WorkerFactory factory, final ServicePath path)
    {
        WorkerCallback taskCallback =  new CoreWorkerCallback(codec, queue, stats, tasks);
        this.threadPool = Objects.requireNonNull(pool);
        this.callback = new CoreTaskCallback(codec, stats, new WorkerExecutor(path, taskCallback, factory, tasks, threadPool), tasks, queue);
        this.workerQueue = Objects.requireNonNull(queue);
    }


    /**
     * Begin accepting tasks to process.
     * @throws QueueException if the queues cannot be started
     */
    public void start()
        throws QueueException
    {
        workerQueue.start(callback);
    }


    /**
     * The current idle time for the worker. If there are any active threads, this is 0. Otherwise it is the
     * difference between the current time and completion time of the last completed task.
     * @return the current idle time in milliseconds
     */
    public long getCurrentIdleTime()
    {
        if ( threadPool.getActiveCount() == 0 ) {
            return System.currentTimeMillis() - stats.getLastTaskFinishedTime();
        } else {
            return 0;   // if we are working, then we are not idle
        }
    }


    public ManagedWorkerQueue getWorkerQueue()
    {
        return this.workerQueue;
    }


    /**
     * @return the current number of tasks accepted by the worker but are not in progress
     */
    public int getBacklogSize()
    {
        return threadPool.getQueue().size();
    }


    public WorkerStats getStats()
    {
        return this.stats;
    }


    /**
     * Called by the queue component to register a new task incoming.
     */
    private static class CoreTaskCallback implements TaskCallback
    {
        private final Codec codec;
        private final WorkerStats stats;
        private final WorkerExecutor executor;
        private final Map<String, Future<?>> taskMap;
        private final ManagedWorkerQueue workerQueue;


        public CoreTaskCallback(final Codec codec, final WorkerStats stats, final WorkerExecutor executor, final Map<String, Future<?>> tasks, final ManagedWorkerQueue workerQueue)
        {
            this.codec = Objects.requireNonNull(codec);
            this.stats = Objects.requireNonNull(stats);
            this.executor = Objects.requireNonNull(executor);
            this.taskMap = Objects.requireNonNull(tasks);
            this.workerQueue = Objects.requireNonNull(workerQueue);
        }


        /**
         * {@inheritDoc}
         *
         * Use the factory to get a new worker to handle the task, wrap this in a handler
         * and hand it off to the thread pool.
         */
        @Override
        public void registerNewTask(final String queueMsgId, final byte[] taskMessage)
            throws InvalidTaskException, TaskRejectedException
        {
            Objects.requireNonNull(queueMsgId);
            try {
                stats.incrementTasksReceived();
                stats.getInputSizes().update(taskMessage.length);
                TaskMessage tm = codec.deserialise(taskMessage, TaskMessage.class, DecodeMethod.LENIENT);
                LOG.debug("Received task {} (message id: {})", tm.getTaskId(), queueMsgId);
                if (taskIsActive(tm)) {
                    if (tm.getTo() == null) {
                        LOG.debug("Task {} (message id: {}) has no explicit destination, therefore assuming it is intended for this worker, on input queue {}", tm.getTaskId(), queueMsgId, workerQueue.getInputQueue());
                        executor.executeTask(tm, queueMsgId);
                    } else if (tm.getTo().equalsIgnoreCase(workerQueue.getInputQueue())) {
                        LOG.debug("Task {} (message id: {}) is intended for this worker, on input queue {}", tm.getTaskId(), queueMsgId, workerQueue.getInputQueue());
                        executor.executeTask(tm, queueMsgId);
                    } else {
                        LOG.debug("Task {} (message id: {}) is not intended for this worker: input queue {} does not match message destination queue {}", tm.getTaskId(), queueMsgId, workerQueue.getInputQueue(), tm.getTo());
                        executor.forwardTask(tm, queueMsgId);
                    }
                } else {
                    LOG.debug("Task {} is no longer active. The task message (message id: {}) is not being executed.", tm.getTaskId(), queueMsgId);
                    //TODO - CAF-599
                }
            } catch (CodecException e) {
                stats.incrementTasksRejected();
                throw new InvalidTaskException("Queue data did not deserialise to a TaskMessage", e);
            }
        }


        /**
         * Checks whether a task is still active.
         * @param tm task message to be checked to verify whether the task is still active
         * @return true if the task is still active, false otherwise
         */
        private boolean taskIsActive(final TaskMessage tm) {
            //TODO - CAF-599
            return true;
        }


        /**
         * Cancel all the Future objects in our Map of running tasks. If the task is not yet
         * running it will just be thrown out of the queue. If it has completed this has no
         * effect. If it is running the Thread will be interrupted.
         */
        @Override
        public void abortTasks()
        {
            LOG.warn("Aborting all current queued and in-progress tasks");
            taskMap.forEach((key, value) -> {
                value.cancel(true);
                stats.incrementTasksAborted();
            });
            taskMap.clear();
        }
    }


    /**
     * Called by a WorkerWrapper to indicate a task was completed by a worker.
     */
    private static class CoreWorkerCallback implements WorkerCallback
    {
        private final Codec codec;
        private final WorkerQueue workerQueue;
        private final WorkerStats stats;
        private final ConcurrentMap<String, Future<?>> taskMap;


        public CoreWorkerCallback(final Codec codec, final WorkerQueue queue, final WorkerStats stats, final ConcurrentMap<String, Future<?>> tasks)
        {
            this.codec = Objects.requireNonNull(codec);
            this.workerQueue = Objects.requireNonNull(queue);
            this.stats = Objects.requireNonNull(stats);
            this.taskMap = Objects.requireNonNull(tasks);
        }


        /**
         * {@inheritDoc}
         *
         * Hand off the serialised result from a worker with its status to the queue. If the result cannot
         * be serialised for any reason, we reject the task.
         */
        @Override
        public void complete(final String queueMsgId, final String queue, final TaskMessage responseMessage)
        {
            Objects.requireNonNull(queueMsgId);
            Objects.requireNonNull(queue);
            Objects.requireNonNull(responseMessage);
            taskMap.remove(queueMsgId);
            LOG.debug("Task {} complete (message id: {})", responseMessage.getTaskId(), queueMsgId);
            LOG.debug("Setting destination {} in task {} (message id: {})", queue, responseMessage.getTaskId(), queueMsgId);
            responseMessage.setTo(queue);
            try {
                byte[] output = codec.serialise(responseMessage);
                workerQueue.publish(queueMsgId, output, queue);
                stats.getOutputSizes().update(output.length);
                stats.updatedLastTaskFinishedTime();
                if ( TaskStatus.isSuccessfulResponse(responseMessage.getTaskStatus()) ) {
                    stats.incrementTasksSucceeded();
                } else {
                    stats.incrementTasksFailed();
                }
            } catch (CodecException | QueueException e) {
                LOG.error("Cannot publish data for task {}, rejecting", responseMessage.getTaskId(), e);
                abandon(queueMsgId);
            }
        }


        @Override
        public void abandon(final String queueMsgId)
        {
            LOG.debug("Rejecting message id {}", queueMsgId);
            workerQueue.rejectTask(queueMsgId);
            stats.incrementTasksRejected();
        }


        @Override
        public void forward(String queueMsgId, String queue, TaskMessage forwardedMessage) {
            Objects.requireNonNull(queueMsgId);
            Objects.requireNonNull(queue);
            Objects.requireNonNull(forwardedMessage);
            taskMap.remove(queueMsgId);
            LOG.debug("Task {} (message id: {}) being forwarded to queue {}", forwardedMessage.getTaskId(), queueMsgId, queue);
            try {
                byte[] output = codec.serialise(forwardedMessage);
                workerQueue.publish(queueMsgId, output, queue);
                stats.incrementTasksForwarded();
                //TODO - I'm guessing this stat should not be updated for forward messages:
                // stats.getOutputSizes().update(output.length);
            } catch (CodecException | QueueException e) {
                LOG.error("Cannot publish data for forwarded task {}, rejecting", forwardedMessage.getTaskId(), e);
                abandon(queueMsgId);
            }
        }


        @Override
        public void discard(String queueMsgId) {
            LOG.debug("Discarding message id {}", queueMsgId);
            stats.incrementTasksDiscarded();
        }
    }
}
