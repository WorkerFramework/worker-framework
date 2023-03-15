/*
 * Copyright 2015-2023 Open Text.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.api.worker;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.NotNull;

/**
 * The generic task message class to be serialised from or to a queue. This will contain the serialised worker-specific data inside.
 */
public final class QueueTaskMessage
{
    public static final int TASK_MESSAGE_VERSION = 4;

    /**
     * The version of this TaskMessage wrapper.
     */
    private int version = TASK_MESSAGE_VERSION;

    /**
     * Unique id for this task chain.
     */
    @NotNull
    private String taskId;

    /**
     * Identifies the sort of task this message is.
     */
    @NotNull
    private String taskClassifier;

    /**
     * The numeric API version of the message task.
     */
    @NotNull
    private int taskApiVersion;

    /**
     * The serialised data of the task-specific message.
     */
    @NotNull
    private Object taskData;

    /**
     * Status of this task.
     */
    @NotNull
    private TaskStatus taskStatus;

    /**
     * Holds worker-specific context data.
     */
    @NotNull
    private Map<String, byte[]> context;

    /**
     * The destination pipe to which the sender intends the message to be sent.
     */
    private String to;

    /**
     * Additional fields used in tracking task messages.
     */
    private TrackingInfo tracking;

    /**
     * Information about the source of the task message.
     */
    private TaskSourceInfo sourceInfo;

    /**
     * Task message priority.
     */
    private Integer priority;

    /**
     * This field contains the correlation id which is eventually logged via MDC.
     */
    private String correlationId;

    public QueueTaskMessage()
    {
    }

    public QueueTaskMessage(final String taskId, final String taskClassifier, final int taskApiVersion, final Object taskData,
                            final TaskStatus taskStatus, final Map<String, byte[]> context)
    {
        this(taskId, taskClassifier, taskApiVersion, taskData, taskStatus, context, null);
    }

    public QueueTaskMessage(final String taskId, final String taskClassifier, final int taskApiVersion, final Object taskData,
                            final TaskStatus taskStatus, final Map<String, byte[]> context, final String to)
    {
        this(taskId, taskClassifier, taskApiVersion, taskData, taskStatus, context, to, null);
    }

    public QueueTaskMessage(final String taskId, final String taskClassifier, final int taskApiVersion, final Object taskData,
                            final TaskStatus taskStatus, final Map<String, byte[]> context, final String to, final TrackingInfo tracking)
    {
        this(taskId, taskClassifier, taskApiVersion, taskData, taskStatus, context, to, tracking, null);
    }

    public QueueTaskMessage(final String taskId, final String taskClassifier, final int taskApiVersion, final Object taskData,
                            final TaskStatus taskStatus, final Map<String, byte[]> context, final String to, final TrackingInfo tracking,
                            final TaskSourceInfo sourceInfo)
    {
        this(taskId, taskClassifier, taskApiVersion, taskData, taskStatus, context, to, tracking, sourceInfo, null);
    }

    public QueueTaskMessage(final String taskId, final String taskClassifier, final int taskApiVersion, final Object taskData,
                            final TaskStatus taskStatus, final Map<String, byte[]> context, final String to, final TrackingInfo tracking,
                            final TaskSourceInfo sourceInfo, final String correlationId)
    {
        this.taskId = Objects.requireNonNull(taskId);
        this.taskClassifier = Objects.requireNonNull(taskClassifier);
        this.taskApiVersion = Objects.requireNonNull(taskApiVersion);
        this.taskData = Objects.requireNonNull(taskData);
        this.taskStatus = Objects.requireNonNull(taskStatus);
        this.context = Objects.requireNonNull(context);
        this.to = to;
        this.tracking = tracking;
        this.sourceInfo = sourceInfo;
        this.correlationId = correlationId;
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

    public Object getTaskData()
    {
        return taskData;
    }

    public void setTaskData(final Object taskData)
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

    public String getTo()
    {
        return to;
    }

    public void setTo(final String to)
    {
        this.to = to;
    }

    public TrackingInfo getTracking()
    {
        return tracking;
    }

    public void setTracking(final TrackingInfo tracking)
    {
        this.tracking = tracking;
    }

    public TaskSourceInfo getSourceInfo()
    {
        return sourceInfo;
    }

    public void setSourceInfo(final TaskSourceInfo sourceInfo)
    {
        this.sourceInfo = sourceInfo;
    }

    public Integer getPriority()
    {
        return priority;
    }

    public void setPriority(final Integer priority)
    {
        this.priority = priority;
    }

    public String getCorrelationId()
    {
        return correlationId;
    }

    public void setCorrelationId(final String correlationId)
    {
        this.correlationId = correlationId;
    }
}
