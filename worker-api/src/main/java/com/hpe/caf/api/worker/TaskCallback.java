package com.hpe.caf.api.worker;


import java.util.Map;

/**
 * A callback interface used to announce the arrival of a new task for a worker
 * to process or signal that the core should cancel its tasks. Generally called
 * from a WorkerQueue implementation.
 * @since 7.0
 */
public interface TaskCallback
{
    /**
     * Announce to the worker core that a new task has been picked off the queue for
     * processing.
     * @param taskId an arbitrary task reference
     * @param taskData the task data that is specific to the workers hosted
     * @param headers the map of key/value paired headers on the message
     * @throws TaskRejectedException if the worker framework rejected execution of the task at this time
     * @throws InvalidTaskException if the worker framework indicates this task is invalid and cannot possibly be executed
     */
    void registerNewTask(String taskId, byte[] taskData, Map<String, Object> headers)
        throws TaskRejectedException, InvalidTaskException;


    /**
     * Signal that any tasks queued or in operation should be aborted. This usually
     * means there was a problem with the queue and any accepted messages should be
     * considered void.
     */
    void abortTasks();
}
