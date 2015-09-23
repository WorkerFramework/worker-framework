package com.hpe.caf.api.worker;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * The generic task message class to be serialised from or to a queue.
 * This will contain the serialised worker-specific data inside.
 */
public final class TaskMessage
{
    public static final int TASK_MESSAGE_VERSION = 1;
    /**
     * The version of this TaskMessage wrapper.
     */
    private int version = TASK_MESSAGE_VERSION;
    /**
     * Unique id for this task chain.
     */
    private String taskId;
    /**
     * Identifies the sort of task this message is.
     */
    private String taskClassifier;
    /**
     * The numeric API version of the message task.
     */
    private int taskApiVersion;
    /**
     * The serialised data of the task-specific message.
     */
    private byte[] taskData;
    /**
     * Status of this task.
     */
    private TaskStatus taskStatus;
    /**
     * Holds worker-specific context data.
     */
    private Map<String, byte[]> context;


    public TaskMessage(){ }


    public TaskMessage(final String taskId, final String taskClassifier, final int taskApiVersion, final byte[] taskData,
                       final TaskStatus taskStatus, final Map<String, byte[]> context)
    {
        this.taskId = Objects.requireNonNull(taskId);
        this.taskClassifier = Objects.requireNonNull(taskClassifier);
        this.taskApiVersion = Objects.requireNonNull(taskApiVersion);
        this.taskData = Objects.requireNonNull(taskData);
        this.taskStatus = Objects.requireNonNull(taskStatus);
        this.context = Objects.requireNonNull(context);
    }


    public int getVersion()
    {
        return version;
    }


    public void setVersion(final int version)
    {
        this.version = version;
    }


    public String getTaskId()
    {
        return taskId;
    }


    public void setTaskId(final String taskId)
    {
        this.taskId = taskId;
    }


    public String getTaskClassifier()
    {
        return taskClassifier;
    }


    public void setTaskClassifier(final String taskClassifier)
    {
        this.taskClassifier = taskClassifier;
    }


    public byte[] getTaskData()
    {
        return taskData;
    }


    public void setTaskData(final byte[] taskData)
    {
        this.taskData = taskData;
    }


    public TaskStatus getTaskStatus()
    {
        return taskStatus;
    }


    public void setTaskStatus(final TaskStatus taskStatus)
    {
        this.taskStatus = taskStatus;
    }


    public int getTaskApiVersion()
    {
        return taskApiVersion;
    }


    public void setTaskApiVersion(final int taskApiVersion)
    {
        this.taskApiVersion = taskApiVersion;
    }


    public Map<String, byte[]> getContext()
    {
        return context == null ? new HashMap<>() : context;
    }


    public void setContext(final Map<String, byte[]> context)
    {
        this.context = context;
    }
}
