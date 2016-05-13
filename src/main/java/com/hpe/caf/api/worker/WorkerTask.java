package com.hpe.caf.api.worker;

/**
 * Represents a task to be completed by the CAF Worker.
 */
public interface WorkerTask {

    /**
     * Retrieves an indicator of the type of the task
     */
    String getClassifier();

    /**
     * Retrieves the version of the task message used
     */
    int getVersion();

    /**
     * Retrieves the task status
     */
    TaskStatus getStatus();

    /**
     * Retrieves the actual task data in a serialised form
     */
    byte[] getData();

    /**
     * Retrieves any task specific context associated with the task
     */
    byte[] getContext();

    /**
     * Retrieves tracking information associated with the task
     */
    TrackingInfo getTrackingInfo();

    /**
     * Retrieves information relating to the source of the task
     */
    TaskSourceInfo getSourceInfo();

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
