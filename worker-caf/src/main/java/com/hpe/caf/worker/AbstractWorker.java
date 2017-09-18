/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.worker;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskFailedException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Objects;
import java.util.Set;

/**
 * A partial Worker implementation with utility methods.
 *
 * @param <T> the task class for this Worker
 * @param <V> the result class for this Worker
 */
public abstract class AbstractWorker<T, V> implements Worker
{
    private final T task;
    private final String resultQueue;
    private final Codec codec;
    private static final Logger LOG = LoggerFactory.getLogger(AbstractWorker.class);
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * Create a Worker. The input task will be validated.
     *
     * @param task the input task for this Worker to operate on
     * @param resultQueue the reference to the queue that should take results from this type of Worker. This can be null if no resultQueue
     * is provided for this type of worker
     * @param codec used to serialising result data
     * @throws InvalidTaskException if the input task does not validate successfully
     */
    public AbstractWorker(final T task, final String resultQueue, final Codec codec)
        throws InvalidTaskException
    {
        this.task = Objects.requireNonNull(task);
        this.resultQueue = resultQueue; // resultQueue can be null for a dead end worker
        this.codec = Objects.requireNonNull(codec);
        Set<ConstraintViolation<T>> violations = validator.validate(task);
        if (violations.size() > 0) {
            LOG.error("Task of type {} failed validation due to: {}", task.getClass().getSimpleName(), violations);
            throw new InvalidTaskException("Task failed validation");
        }
    }

    @Override
    public final WorkerResponse getGeneralFailureResult(final Throwable t)
    {
        return new WorkerResponse(getResultQueue(), TaskStatus.RESULT_EXCEPTION, getExceptionData(t), getWorkerIdentifier(), getWorkerApiVersion(), null);
    }

    /**
     * @return the task for this Worker to operate on
     */
    protected final T getTask()
    {
        return this.task;
    }

    /**
     * @return the reference to the queue that should contain the results from this type of Worker
     */
    protected final String getResultQueue()
    {
        return this.resultQueue;
    }

    /**
     * @return the Codec supplied to the Worker from the framework
     */
    protected final Codec getCodec()
    {
        return this.codec;
    }

    /**
     * Utility method for creating a WorkerReponse that represents a successful result.
     *
     * @param result the result from the Worker
     * @return a WorkerResponse that represents a successful result containing the specified task-specific serialised message
     */
    protected final WorkerResponse createSuccessResult(final V result)
    {
        return createSuccessResult(result, null);
    }

    /**
     * Utility method for creating a WorkerReponse that represents a successful result with context data.
     *
     * @param result the result from the Worker
     * @param context the context entries to add to the published message
     * @return a WorkerResponse that represents a successful result containing the specified task-specific serialised message
     */
    protected final WorkerResponse createSuccessResult(final V result, final byte[] context)
    {
        try {
            byte[] data = (result != null ? getCodec().serialise(result) : new byte[]{});
            return new WorkerResponse(getResultQueue(), TaskStatus.RESULT_SUCCESS, data, getWorkerIdentifier(), getWorkerApiVersion(), context);
        } catch (CodecException e) {
            throw new TaskFailedException("Failed to serialise result", e);
        }
    }

    /**
     * Utility method for creating a WorkerReponse that represents a success, but does not send a message to the worker's output message.
     *
     * @return a WorkerResponse that represents a success
     */
    protected final WorkerResponse createSuccessNoOutputToQueue()
    {
        return new WorkerResponse(null, TaskStatus.RESULT_SUCCESS, new byte[]{}, getWorkerIdentifier(), getWorkerApiVersion(), null);
    }

    /**
     * Utility method for creating a WorkerReponse that represents a failed result.
     *
     * @param result the result from the Worker
     * @return a WorkerResponse that represents a failed result containing the specified task-specific serialised message
     */
    protected final WorkerResponse createFailureResult(final V result)
    {
        return createFailureResult(result, null);
    }

    /**
     * Utility method for creating a WorkerReponse that represents a failed result with context data.
     *
     * @param result the result from the Worker
     * @param context the context entries to add to the published message
     * @return a WorkerResponse that represents a failed result containing the specified task-specific serialised message
     */
    protected final WorkerResponse createFailureResult(final V result, final byte[] context)
    {
        try {
            byte[] data = (result != null ? getCodec().serialise(result) : new byte[]{});
            return new WorkerResponse(getResultQueue(), TaskStatus.RESULT_FAILURE, data, getWorkerIdentifier(), getWorkerApiVersion(), context);
        } catch (CodecException e) {
            throw new TaskFailedException("Failed to serialise result", e);
        }
    }

    /**
     * Utility method for creating a new task submission to an arbitrary queue. This is for Workers that are chaining jobs to other
     * Workers.
     *
     * @param queue the reference of the queue to put the message on
     * @param data the serialised task-specific message for the Worker to perform the work
     * @param messageIdentifier the classifier for the task-specific message
     * @param messageApiVersion the API version for the task-specific message
     * @return a WorkerResponse that represents a new task submission to a specific queue
     */
    protected final WorkerResponse createTaskSubmission(final String queue, final byte[] data, final String messageIdentifier, final int messageApiVersion)
    {
        return createTaskSubmission(queue, data, messageIdentifier, messageApiVersion, null);
    }

    /**
     * Utility method for creating a new task submission to an arbitrary queue with context data. This is for Workers that are chaining
     * jobs to other Workers.
     *
     * @param queue the reference of the queue to put the message on
     * @param data the serialised task-specific message for the Worker to perform the work
     * @param messageIdentifier the classifier for the task-specific message
     * @param messageApiVersion the API version for the task-specific message
     * @param context the context entries to add to the published message
     * @return a WorkerResponse that represents a new task submission to a specific queue
     */
    protected final WorkerResponse createTaskSubmission(final String queue, final byte[] data, final String messageIdentifier, final int messageApiVersion,
                                                        final byte[] context)
    {
        return new WorkerResponse(queue, TaskStatus.NEW_TASK, data, messageIdentifier, messageApiVersion, context);
    }

    /**
     * Utility method to check the interrupted flag of the current Thread and throw InterruptedException if true.
     *
     * @throws InterruptedException if the current Thread is interrupted
     */
    protected final void checkIfInterrupted()
        throws InterruptedException
    {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Worker interrupted");
        }
    }

    /**
     * @param t the Throwable from the Worker
     * @return a byte array that is either the serialised Class of the Throwable or empty
     */
    private byte[] getExceptionData(final Throwable t)
    {
        try {
            String exceptionDetail = buildExceptionStackTrace(t);
            return getCodec().serialise(exceptionDetail);
        } catch (CodecException e) {
            LOG.warn("Failed to serialise exception, continuing", e);
            return new byte[]{};
        }
    }

    /**
     * Builds up a stack trace with one level of cause stack trace
     *
     * @param e The exception to build a stack trace from
     * @return Stack trace constructed from exception
     */
    protected String buildExceptionStackTrace(Throwable e)
    {
        // Build up exception detail from stack trace
        StringBuilder exceptionStackTrace = new StringBuilder(e.getClass() + " " + e.getMessage());
        // Check if there is a stack trace on the exception before building it up into a string
        if (Objects.nonNull(e.getStackTrace())) {
            exceptionStackTrace.append(stackTraceToString(e.getStackTrace()));
        }
        // If a cause exists add it to the exception detail
        if (Objects.nonNull(e.getCause())) {
            exceptionStackTrace.append(". Cause: " + e.getCause().getClass().toString() + " "
                + e.getCause().getMessage());
            // Check if the cause has a stack trace before building it up into a string
            if (Objects.nonNull(e.getCause().getStackTrace())) {
                exceptionStackTrace.append(stackTraceToString(e.getCause().getStackTrace()));
            }
        }
        return exceptionStackTrace.toString();
    }

    protected String stackTraceToString(StackTraceElement[] stackTraceElements)
    {
        StringBuilder stackTraceStr = new StringBuilder();
        // From each stack trace element, build up the stack trace
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            stackTraceStr.append(" " + stackTraceElement.toString());
        }
        return stackTraceStr.toString();
    }
}
