package com.hpe.caf.api.worker;

/**
 * Provides access onto Worker Task Data and ability to set response.
 */
public interface WorkerTask extends WorkerTaskData {
    /**
     * Used by the Worker to set the response to the task.
     */
    void setResponse(WorkerResponse response);

    /**
     * Used by the Worker to reject the task
     */
    void setResponse(TaskRejectedException taskRejectedException);

    /**
     * Used by the Worker to declare that the task is not valid
     */
    void setResponse(InvalidTaskException invalidTaskException);
}
