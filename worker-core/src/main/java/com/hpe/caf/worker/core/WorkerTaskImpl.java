/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.worker.core;

import com.google.common.base.MoreObjects;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.naming.ServicePath;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WorkerTaskImpl implements WorkerTask
{
    private static final String WORKER_VERSION_UNKNOWN = "UNKNOWN";
    private static final Logger LOG = LoggerFactory.getLogger(WorkerTaskImpl.class);

    private final ServicePath servicePath;
    private final WorkerCallback workerCallback;
    private final WorkerFactory workerFactory;
    private final String messageId;
    private final TaskMessage taskMessage;
    private final MessagePriorityManager priorityManager;
    private int responseCount;
    private final Object responseCountLock;
    private final SingleResponseMessageBuffer singleMessageBuffer;
    private final AtomicInteger currentSubtaskId;
    private final Semaphore subtasksPublishedSemaphore;
    private final boolean poison;

    public WorkerTaskImpl(
        final ServicePath servicePath,
        final WorkerCallback workerCallback,
        final WorkerFactory workerFactory,
        final String messageId,
        final TaskMessage taskMessage,
        final boolean poison,
        final MessagePriorityManager priorityManager
    )
    {
        this.servicePath = servicePath;
        this.workerCallback = workerCallback;
        this.workerFactory = workerFactory;
        this.messageId = messageId;
        this.taskMessage = taskMessage;
        this.priorityManager = Objects.requireNonNull(priorityManager);
        this.responseCount = 0;
        this.responseCountLock = new Object();
        this.singleMessageBuffer = new SingleResponseMessageBuffer();
        this.currentSubtaskId = new AtomicInteger();
        this.subtasksPublishedSemaphore = new Semaphore(0);
        this.poison = poison;
    }

    @Override
    public String getClassifier()
    {
        return taskMessage.getTaskClassifier();
    }

    @Override
    public int getVersion()
    {
        return taskMessage.getTaskApiVersion();
    }

    @Override
    public TaskStatus getStatus()
    {
        return taskMessage.getTaskStatus();
    }

    @Override
    public byte[] getData()
    {
        return taskMessage.getTaskData();
    }

    @Override
    public byte[] getContext()
    {
        return taskMessage.getContext().get(servicePath.toString());
    }

    @Override
    public TrackingInfo getTrackingInfo()
    {
        return taskMessage.getTracking();
    }

    @Override
    public TaskSourceInfo getSourceInfo()
    {
        return taskMessage.getSourceInfo();
    }

    @Override
    public void addResponse(final WorkerResponse response, final boolean includeTaskContext)
    {
        Objects.requireNonNull(response);
        Objects.requireNonNull(response.getQueueReference());

        incrementResponseCount(false);

        final TaskMessage responseMessage = createResponseMessage(includeTaskContext, response);

        singleMessageBuffer.add(responseMessage);
    }

    @Override
    public void setResponse(final WorkerResponse response)
    {
        Objects.requireNonNull(response);

        incrementResponseCount(true);

        final TaskMessage responseMessage = createResponseMessage(true, response);

        completeResponse(responseMessage);
    }

    private TaskMessage createResponseMessage(final boolean includeTaskContext, final WorkerResponse response)
    {
        final Map<String, byte[]> responseContext = createFullResponseContext(includeTaskContext, response.getContext());

        final String responseMessageType = response.getMessageType();

        //  Check if the 'trackTo' field needs to be reset.
        final TrackingInfo trackingInfo;
        if (response.getResetTrackTo()) {
            //  Reset trackTo field to null;
            trackingInfo = taskMessage.getTracking();
            if (trackingInfo != null) {
                trackingInfo.setTrackTo(null);
            }
        } else {
            //  No tracking changes required.
            trackingInfo = taskMessage.getTracking();
        }

        final TaskMessage responseMessage = new TaskMessage(
            taskMessage.getTaskId(), responseMessageType,
            response.getApiVersion(), response.getData(),
            response.getTaskStatus(), responseContext,
            response.getQueueReference(), trackingInfo,
            new TaskSourceInfo(getWorkerName(responseMessageType), getWorkerVersion()));
        responseMessage.setPriority(priorityManager.getResponsePriority(taskMessage));

        return responseMessage;
    }

    @Override
    public void setResponse(final TaskRejectedException taskRejectedException)
    {
        incrementResponseCount(true);

        LOG.info("Worker requested to abandon task {} (message id: {})",
                 taskMessage.getTaskId(), taskMessage, taskRejectedException);

        workerCallback.abandon(messageId, taskRejectedException);
    }

    @Override
    public void setResponse(final InvalidTaskException invalidTaskException)
    {
        if (invalidTaskException == null) {
            throw new IllegalArgumentException();
        }

        incrementResponseCount(true);

        LOG.error("Task data is invalid for {}, returning status {}",
                  taskMessage.getTaskId(), TaskStatus.INVALID_TASK, invalidTaskException);

        final String taskClassifier = MoreObjects.firstNonNull(taskMessage.getTaskClassifier(), "");

        final String invalidTaskExceptionMessage = invalidTaskException.getMessage();
        final byte[] taskData
            = invalidTaskExceptionMessage == null
                ? new byte[]{} : invalidTaskExceptionMessage.getBytes(StandardCharsets.UTF_8);

        final Map<String, byte[]> context = MoreObjects.firstNonNull(
            taskMessage.getContext(),
            Collections.<String, byte[]>emptyMap());

        final TaskMessage invalidResponse = new TaskMessage(
            MoreObjects.firstNonNull(taskMessage.getTaskId(), ""),
            taskClassifier,
            taskMessage.getTaskApiVersion(),
            taskData,
            TaskStatus.INVALID_TASK,
            context,
            workerFactory.getInvalidTaskQueue(),
            taskMessage.getTracking(),
            new TaskSourceInfo(getWorkerName(taskClassifier), getWorkerVersion()));

        completeResponse(invalidResponse);
    }

    public Worker createWorker()
        throws InvalidTaskException, TaskRejectedException
    {
        return workerFactory.getWorker(this);
    }

    public void logInterruptedException(final InterruptedException interruptedException)
    {
        LOG.warn("Worker interrupt signalled, not performing callback for task {} (message id: {})",
                 taskMessage.getTaskId(), messageId, interruptedException);
    }

    public boolean isResponseSet()
    {
        return (responseCount < 0);
    }

    /**
     * Checks that the response count hasn't been finalised, and adds one to it if it hasn't been.
     */
    private void incrementResponseCount(final boolean isFinalResponse)
    {
        synchronized (responseCountLock) {
            final int rc = responseCount + 1;
            if (rc <= 0) {
                throw new RuntimeException("Final response already set!");
            }

            responseCount = isFinalResponse ? -rc : rc;
        }
    }

    /**
     * Returns the final response count if it has been established, or throws an exception if it hasn't.
     */
    private int getFinalResponseCount()
    {
        final int rc = responseCount;
        if (rc >= 0) {
            throw new RuntimeException("Final response count not yet known!");
        }

        return -rc;
    }

    public boolean isPoison()
    {
        return poison;
    }

    private String getWorkerName(final String defaultName)
    {
        final com.hpe.caf.api.worker.WorkerConfiguration workerConfig = workerFactory.getWorkerConfiguration();

        if (workerConfig != null) {
            final String workerName = workerConfig.getWorkerName();

            if (workerName != null) {
                return workerName;
            }
        }

        return defaultName;
    }

    private String getWorkerVersion()
    {
        final com.hpe.caf.api.worker.WorkerConfiguration workerConfig = workerFactory.getWorkerConfiguration();

        if (workerConfig != null) {
            final String workerVersion = workerConfig.getWorkerVersion();

            if (workerVersion != null) {
                return workerVersion;
            }
        }

        return WORKER_VERSION_UNKNOWN;
    }

    /**
     * Creates a copy of the context (optionally), and updates it with the specified entry if it is non-null.
     */
    private Map<String, byte[]> createFullResponseContext(
        final boolean includeTaskContext,
        final byte[] responseContext
    )
    {
        final Map<String, byte[]> context = includeTaskContext
            ? new HashMap<>(taskMessage.getContext())
            : new HashMap<>();

        if (responseContext != null) {
            context.put(servicePath.toString(), responseContext);
        }

        return context;
    }

    /**
     * Holds a single message back from being published so that it could potentially be made into the final completion message (although
     * the functionality to do that is not written yet).
     */
    private final class SingleResponseMessageBuffer
    {
        private TaskMessage buffer;
        private final Object bufferLock;
        private boolean isBuffering;

        public SingleResponseMessageBuffer()
        {
            this.buffer = null;
            this.bufferLock = new Object();
            this.isBuffering = true;
        }

        /**
         * Add a new message into the buffer, publishing any message that is currently in it to make room.
         */
        public void add(final TaskMessage newResponse)
        {
            Objects.requireNonNull(newResponse);
            addOrFlush(newResponse);
        }

        /**
         * Flush the buffer and turn it off. Any subsequent calls to add() will not be buffered.
         */
        public void flush()
        {
            isBuffering = false;
            addOrFlush(null);
        }

        private void addOrFlush(final TaskMessage newResponse)
        {
            final TaskMessage previousResponse;
            final boolean newResponseBuffered;

            // Get the previous message out of the buffer,
            // and if buffering is still enabled then put the new message into it
            synchronized (bufferLock) {
                previousResponse = buffer;
                buffer = (newResponseBuffered = isBuffering) ? newResponse : null;
            }

            // Publish the previous message if there is one
            publishSubtask(previousResponse);

            // Publish the new message if it was not buffered
            if (!newResponseBuffered) {
                // Append the subtask id to the task id
                publishSubtask(newResponse);
            }
        }
    }

    /**
     * Gets the next subtask identifier, appends it to the message and publishes it.
     */
    private void publishSubtask(final TaskMessage responseMessage)
    {
        // Do not do anything if the message is null
        if (responseMessage == null) {
            return;
        }

        // Get the next subtask identifier
        final int subtaskId = currentSubtaskId.incrementAndGet();

        // Append the subtask id to the task id
        updateTaskId(responseMessage, subtaskId, false);

        // Publish this message
        workerCallback.send(messageId, responseMessage);

        // Allow the final task to complete (in case it was blocking waiting on the message to be published)
        subtasksPublishedSemaphore.release();
    }

    /**
     * Finalises the final response message and publishes it (after any subtasks).
     */
    private void completeResponse(final TaskMessage responseMessage)
    {
        // Get the final number of responses
        final int finalResponseCount = getFinalResponseCount();

        // Check if there are multiple responses
        if (finalResponseCount > 1) {
            // Ensure the buffer is flushed
            singleMessageBuffer.flush();

            // Add a suffix the task id
            updateTaskId(responseMessage, finalResponseCount, true);

            // Ensure that all the subtasks have been published before continuing
            subtasksPublishedSemaphore.acquireUninterruptibly(finalResponseCount - 1);
        }

        // Complete the task
        workerCallback.complete(messageId, responseMessage.getTo(), responseMessage);
    }

    /**
     * Updates the specified {@link TaskMessage} with the specified subtask identifier.
     */
    private void updateTaskId(final TaskMessage responseMessage, final int subtaskId, final boolean isFinalResponse)
    {
        // Put together the suffix to be added
        final String subtaskSuffix;
        {
            final StringBuilder builder = new StringBuilder();
            builder.append('.');
            builder.append(subtaskId);
            if (isFinalResponse) {
                builder.append('*');
            }

            subtaskSuffix = builder.toString();
        }

        // Update the task id
        responseMessage.setTaskId(taskMessage.getTaskId() + subtaskSuffix);

        // Update the tracking info
        final TrackingInfo taskMessageTracking = taskMessage.getTracking();
        if (taskMessageTracking != null) {
            final String trackingTaskId = taskMessageTracking.getJobTaskId();

            if (trackingTaskId != null) {
                final TrackingInfo trackingInfo = new TrackingInfo(taskMessageTracking);
                trackingInfo.setJobTaskId(trackingTaskId + subtaskSuffix);

                responseMessage.setTracking(trackingInfo);
            }
        }
    }
}
