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
    
    /**
     * Used by the Worker to determine that a message is poison and cannot be processed by the
     * worker as it has failed and or crashed the worker on number of occasions previously
     * 
     * @return boolean if a message is poisoned
     */
    boolean isPoison();
}
