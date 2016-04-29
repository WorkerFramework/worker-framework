package com.hpe.caf.worker.core;

import com.google.common.base.MoreObjects;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerCallback;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.api.worker.WorkerTask;
import com.hpe.caf.naming.ServicePath;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WorkerTaskImpl implements WorkerTask
{
    private static final Logger LOG = LoggerFactory.getLogger(WorkerTaskImpl.class);

    private final ServicePath servicePath;
    private final WorkerCallback workerCallback;
    private final WorkerFactory workerFactory;
    private final String messageId;
    private final TaskMessage taskMessage;
    private final AtomicBoolean isResponseSet;

    public WorkerTaskImpl
    (
        final ServicePath servicePath,
        final WorkerCallback workerCallback,
        final WorkerFactory workerFactory,
        final String messageId,
        final TaskMessage taskMessage
    ) {
        this.servicePath = servicePath;
        this.workerCallback = workerCallback;
        this.workerFactory = workerFactory;
        this.messageId = messageId;
        this.taskMessage = taskMessage;
        this.isResponseSet = new AtomicBoolean();
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
    public void setResponse(final WorkerResponse response) {
        ensureSingleResponse();

        final Map<String, byte[]> responseContext = taskMessage.getContext();
        final byte[] workerResponseContext = response.getContext();
        if (workerResponseContext != null) {
            responseContext.put(servicePath.toString(), workerResponseContext);
        }

        final TaskMessage responseMessage = new TaskMessage(
            taskMessage.getTaskId(), response.getMessageType(),
            response.getApiVersion(), response.getData(),
            response.getTaskStatus(), responseContext,
            response.getQueueReference(), taskMessage.getTracking());

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
        final byte[] taskData = new byte[] {};
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
        return workerFactory.getWorker(
            getClassifier(), getVersion(), getStatus(), getData(), getContext(), getTrackingInfo());
    }

    public void logInterruptedException(final InterruptedException interruptedException) {
        LOG.warn("Worker interrupt signalled, not performing callback for task {} (message id: {})",
            taskMessage.getTaskId(), messageId, interruptedException);
    }

    /**
     * Throws an exception if a response has already been received
     */
    private void ensureSingleResponse() {
        if (isResponseSet.getAndSet(true)) {
            throw new RuntimeException("Response already set!");
        }
    }
}
