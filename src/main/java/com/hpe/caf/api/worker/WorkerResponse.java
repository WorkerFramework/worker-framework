package com.hpe.caf.api.worker;


import java.util.Objects;


/**
 * Object to represent a response from a Worker, to be interpreted by the core worker framework.
 */
public class WorkerResponse
{
    private final String queueReference;
    private final TaskStatus taskStatus;
    private final byte[] data;
    private final String messageType;
    private final int apiVersion;
    private final byte[] context;


    /**
     * Create a new WorkerResponse.
     * @param queue the reference to the queue that the response data should be put upon
     * @param status the status of the message the Worker is returning
     * @param data the serialised task-specific data returned from the Worker internals
     * @param msgType the task-specific message classifier
     * @param version the task-specific message API version
     * @param context the new context to add to the task message, can be null
     */
    public WorkerResponse(final String queue, final TaskStatus status, final byte[] data, final String msgType, final int version, final byte[] context)
    {
        this.queueReference = Objects.requireNonNull(queue);
        this.taskStatus = Objects.requireNonNull(status);
        this.data = data;
        this.messageType = Objects.requireNonNull(msgType);
        this.apiVersion = version;
        this.context = context;
    }


    public TaskStatus getTaskStatus()
    {
        return taskStatus;
    }


    public String getQueueReference()
    {
        return queueReference;
    }


    public byte[] getData()
    {
        return data;
    }


    public String getMessageType()
    {
        return messageType;
    }


    public int getApiVersion()
    {
        return apiVersion;
    }


    public byte[] getContext()
    {
        return context;
    }
}
