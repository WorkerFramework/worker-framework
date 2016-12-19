/*
 * (c) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final AtomicBoolean isResponseSet;
    private final boolean poison;
    
    public WorkerTaskImpl
    (
        final ServicePath servicePath,
        final WorkerCallback workerCallback,
        final WorkerFactory workerFactory,
        final String messageId,
        final TaskMessage taskMessage,
        final boolean poison
    ) {
        this.servicePath = servicePath;
        this.workerCallback = workerCallback;
        this.workerFactory = workerFactory;
        this.messageId = messageId;
        this.taskMessage = taskMessage;
        this.isResponseSet = new AtomicBoolean();
        this.poison = poison;
    }

    @Override
    public String getClassifier() {
        return taskMessage.getTaskClassifier();
    }

    @Override
    public int getVersion() {
        return taskMessage.getTaskApiVersion();
    }

    @Override
    public TaskStatus getStatus() {
        return taskMessage.getTaskStatus();
    }

    @Override
    public byte[] getData() {
        return taskMessage.getTaskData();
    }

    @Override
    public byte[] getContext() {
        return taskMessage.getContext().get(servicePath.toString());
    }

    @Override
    public TrackingInfo getTrackingInfo() {
        return taskMessage.getTracking();
    }

    @Override
    public TaskSourceInfo getSourceInfo() {
        return taskMessage.getSourceInfo();
    }

    @Override
    public void setResponse(final WorkerResponse response) {
        ensureSingleResponse();

        final Map<String, byte[]> responseContext = taskMessage.getContext();
        final byte[] workerResponseContext = response.getContext();
        if (workerResponseContext != null) {
            responseContext.put(servicePath.toString(), workerResponseContext);
        }

        final String responseMessageType = response.getMessageType();

        final TaskMessage responseMessage = new TaskMessage(
            taskMessage.getTaskId(), responseMessageType,
            response.getApiVersion(), response.getData(),
            response.getTaskStatus(), responseContext,
            response.getQueueReference(), taskMessage.getTracking(),
            new TaskSourceInfo(getWorkerName(responseMessageType), getWorkerVersion()));

        workerCallback.complete(
            messageId, response.getQueueReference(), responseMessage);
    }

    @Override
    public void setResponse(final TaskRejectedException taskRejectedException) {
        ensureSingleResponse();

        LOG.info("Worker requested to abandon task {} (message id: {})",
            taskMessage.getTaskId(), taskMessage, taskRejectedException);

        workerCallback.abandon(messageId);
    }

    @Override
    public void setResponse(final InvalidTaskException invalidTaskException) {
        ensureSingleResponse();

        LOG.error("Task data is invalid for {}, returning status {}",
            taskMessage.getTaskId(), TaskStatus.INVALID_TASK, invalidTaskException);

        final String taskId =
            MoreObjects.firstNonNull(taskMessage.getTaskId(), "");
        final String taskClassifier =
            MoreObjects.firstNonNull(taskMessage.getTaskClassifier(), "");
        final int taskApiVersion = taskMessage.getTaskApiVersion();
        final String invalidTaskExceptionMessage = invalidTaskException.getMessage();
        final byte[] taskData =
                invalidTaskExceptionMessage == null ?
                        new byte[] {} : invalidTaskExceptionMessage.getBytes(StandardCharsets.UTF_8);
        final TaskStatus taskStatus = TaskStatus.INVALID_TASK;
        final Map<String, byte[]> context = MoreObjects.firstNonNull(
            taskMessage.getContext(),
            Collections.<String, byte[]>emptyMap());

        final TaskMessage invalidResponse = new TaskMessage(
            taskId, taskClassifier, taskApiVersion, taskData, taskStatus, context);

        workerCallback.complete(
            messageId, workerFactory.getInvalidTaskQueue(), invalidResponse);
    }

    public Worker createWorker()
        throws InvalidTaskException, TaskRejectedException
    {
        return workerFactory.getWorker(this);
    }

    public void logInterruptedException(final InterruptedException interruptedException) {
        LOG.warn("Worker interrupt signalled, not performing callback for task {} (message id: {})",
            taskMessage.getTaskId(), messageId, interruptedException);
    }

    public boolean isResponseSet() {
        return isResponseSet.get();
    }

    /**
     * Throws an exception if a response has already been received
     */
    private void ensureSingleResponse() {
        if (isResponseSet.getAndSet(true)) {
            throw new RuntimeException("Response already set!");
        }
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
}
