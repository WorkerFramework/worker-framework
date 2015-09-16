package com.hpe.caf.api.worker;


import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


/**
 * An actual worker that does useful operations upon task specific data.
 *
 * A Worker can be constructed in any way as per suits the developer, but should only perform the bare
 * minimum of tasks in the constructor to set itself up to perform the computational work. At some point
 * after construction, the worker-core framework will call through to doWork(), at which point this Worker
 * will be on its own separately managed thread and can start performing useful operations. If the Worker
 * throws an exception from the constructor, this task will be rejected back onto the queue (and eventually
 * it may be dropped, depending upon the WorkerQueue implementation).
 *
 * There are no limits upon time taken for the Worker to perform its task, but it must at some point
 * terminate either via throwing an exception returning from doWork() by returning a WorkerResponse object.
 * The Worker base class has various utility methods for returning a WorkerResponse,
 * such as createSuccessResult, createFailureResult, and createTaskSubmission. Preferably a Worker will
 * always return one of these as opposed to throwing a WorkerException out of the object.
 *
 * A Worker also has the option of returning serialised byte data to be put onto the result message if
 * the Worker throws an unhandled exception. This is effectively a "fallback" response because if
 * doWork() has failed to complete there has not yet been any generated task-specific generated response.
 * If your application workflow is fine with simply knowing the task failed without any specific data it
 * is fine to return an empty byte array from getGeneralFailureResult(), but under no circumstance should
 * that method throw an exception. Hence, it is recommended you construct your "failure" result byte data
 * in the constructor of your Worker.
 *
 * Finally, a Worker has methods to classify the type of work it is performing (an "identifier") and another
 * method that returns the integer API version of the task data. These are typically defined in your shareed
 * package that contains the task and result classes, but are used here for constructing a WorkerResponse.
 * @param <T> the task class for this Worker
 * @param <V> the result class for this Worker
 */
public abstract class Worker<T,V>
{
    private final T task;
    private final String resultQueue;
    private final Codec codec;
    private static final Logger LOG = LoggerFactory.getLogger(Worker.class);


    /**
     * Create a Worker.
     * @param task the input task for this Worker to operate on
     * @param resultQueue the reference to the queue that should take results from this type of Worker
     * @param codec used to serialising result data
     */
    public Worker(final T task, final String resultQueue, final Codec codec)
    {
        this.task = Objects.requireNonNull(task);
        this.resultQueue = Objects.requireNonNull(resultQueue);
        this.codec = Objects.requireNonNull(codec);
    }


    /**
     * Start the work on a task.
     * @return the result of the worker operation, and appropriate result data
     * @throws WorkerException if the worker cannot continue with this task for any reason
     * @throws InterruptedException if the worker is interrupted
     */
     public abstract WorkerResponse doWork()
        throws WorkerException, InterruptedException;


    /**
     * @return a string to uniquely identify the sort of tasks this worker will do
     */
    public abstract String getWorkerIdentifier();


    /**
     * This should return a number that identifies the API version that this worker uses, and should
     * be incremented when the format of the task data (or result data) changes. Internal code-logic
     * changes should not affect the API version.
     * @return a numeral that identifies the API version of the worker
     */
    public abstract int getWorkerApiVersion();


    /**
     * In case of a Worker's doWork() method failing with an unhandled exception, it is expected a
     * Worker should be able to return a general result. This method should only return a static
     * object in order to avoid throwing any more exceptions.
     * @return a response in case of a general unhandled exception failure scenario
     */
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
     * @param result the result from the Worker
     * @return a WorkerResponse that represents a successful result containing the specified task-specific serialised message
     * @throws CodecException if the result cannot be serialised
     */
    protected final WorkerResponse createSuccessResult(final V result)
        throws CodecException
    {
        return createSuccessResult(result, null);
    }


    /**
     * Utility method for creating a WorkerReponse that represents a successful result with context data.
     * @param result the result from the Worker
     * @param context the context entries to add to the published message
     * @return a WorkerResponse that represents a successful result containing the specified task-specific serialised message
     * @throws CodecException if the result cannot be serialised
     */
    protected final WorkerResponse createSuccessResult(final V result, final byte[] context)
        throws CodecException
    {
        byte[] data = ( result != null ? getCodec().serialise(result) : new byte[]{} );
        return new WorkerResponse(getResultQueue(), TaskStatus.RESULT_SUCCESS, data, getWorkerIdentifier(), getWorkerApiVersion(), context);
    }


    /**
     * Utility method for creating a WorkerReponse that represents a failed result.
     * @param result the result from the Worker
     * @return a WorkerResponse that represents a failed result containing the specified task-specific serialised message
     * @throws CodecException if the result cannot be serialised
     */
    protected final WorkerResponse createFailureResult(final V result)
        throws CodecException
    {
        return createFailureResult(result, null);
    }


    /**
     * Utility method for creating a WorkerReponse that represents a failed result with context data.
     * @param result the result from the Worker
     * @param context the context entries to add to the published message
     * @return a WorkerResponse that represents a failed result containing the specified task-specific serialised message
     * @throws CodecException if the result cannot be serialised
     */
    protected  final WorkerResponse createFailureResult(final V result, final byte[] context)
        throws CodecException
    {
        byte[] data = ( result != null ? getCodec().serialise(result) : new byte[]{} );
        return new WorkerResponse(getResultQueue(), TaskStatus.RESULT_FAILURE, data, getWorkerIdentifier(), getWorkerApiVersion(), context);
    }


    /**
     * Utility method for creating a new task submission to an arbitrary queue. This is for Workers that are chaining jobs
     * to other Workers.
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
     * Utility method for creating a new task submission to an arbitrary queue with context data. This is for Workers that are
     * chaining jobs to other Workers.
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
     * @param t the Throwable to attempt to serialise
     * @return a byte array that is either the serialised Throwable or empty
     */
    private byte[] getExceptionData(final Throwable t)
    {
        try {
            return getCodec().serialise(t);
        } catch (CodecException e) {
            LOG.warn("Failed to serialise exception, continuing", e);
            return new byte[]{};
        }
    }
}
