package com.hpe.caf.api.worker;


import com.hpe.caf.api.HealthReporter;


/**
 * A general representation of a queue for the purposes of a worker service.
 *
 * A WorkerQueue should configure itself in the constructor but not start accepting messages
 * until the start() method has been called by the worker-core framework. An implementation
 * should also support rejecting messages back onto the queue to be handled by other targets.
 * The number of retries (if at all) is implementation specific.
 * @since 4.0
 */
public abstract class WorkerQueue implements HealthReporter
{
    private final int maxTasks;


    /**
     * Initialise the worker queue, setting up any necessary connections. Until the start() method is called it is expected
     * that the WorkerQueue will not receive or publish any messages.
     * @param maxTasks the maximum number of tasks the backend can process at once
     * @since 6.0
     */
    public WorkerQueue(final int maxTasks)
    {
        this.maxTasks = maxTasks;
    }


    /**
     * Open queues to start accepting tasks and results.
     * @param callback the callback to use when registering or aborting tasks
     * @throws QueueException if the queue cannot be started
     * @since 6.0
     */
    public abstract void start(final TaskCallback callback)
        throws QueueException;


    /**
     * Acknowledge the original received message but send out a new message to a target queue.
     * @param acknowledgeId the internal queue message id of the message to acknowledge
     * @param taskMessage the message to publish
     * @param targetQueue the queue to put the message upon
     * @throws QueueException if the message cannot be submitted
     * @since 6.0
     */
    public abstract void publish(final String acknowledgeId, final byte[] taskMessage, final String targetQueue)
        throws QueueException;


    /**
     * Called from the asynchronous worker service to notify the queue that it is rejecting a task.
     * It is up to the queue implementation as to whether submit this task to retry or not.
     * @param messageId the queue task id that has been rejected
     */
    public abstract void rejectTask(final String messageId);


    /**
     * Halt the incoming queue so that no more tasks are picked up.
     */
    public abstract void shutdownIncoming();


    /**
     * Terminate all queue operations.
     */
    public abstract void shutdown();


    /**
     * @return the metrics implementation for this WorkerQueue
     */
    public abstract WorkerQueueMetricsReporter getMetrics();


    /**
     * @return the maximum number of tasks the worker-core framework indicated it could handle at once
     */
    protected int getMaxTasks()
    {
        return this.maxTasks;
    }
}
