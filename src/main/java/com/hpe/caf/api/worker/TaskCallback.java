package com.hpe.caf.api.worker;


/**
 * A callback interface used to announce the arrival of a new task for a worker
 * to process or signal that the core should cancel its tasks. Generally called
 * from a WorkerQueue implementation.
 */
public interface TaskCallback
{
    /**
     * Announce to the worker core that a new task has been picked off the queue for
     * processing.
     * @param taskId an arbitrary task reference
     * @param taskData the task data that is specific to the workers hosted
     * @throws WorkerException if the task is rejected or cannot be handed off to the core
     */
    void registerNewTask(final String taskId, final byte[] taskData)
            throws WorkerException;


    /**
     * Signal that any tasks queued or in operation should be aborted. This usually
     * means there was a problem with the queue and any accepted messages should be
     * considered void.
     */
    void abortTasks();
}
