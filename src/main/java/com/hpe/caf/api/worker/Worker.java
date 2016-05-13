package com.hpe.caf.api.worker;


/**
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
 * Finally, a Worker has methods to classify the type of work it is performing (an "identifier") and another
 * method that returns the integer API version of the task data. These are typically defined in your shareed
 * package that contains the task and result classes, but are used here for constructing a WorkerResponse.
 * @since 9.0
 */
public interface Worker
{
    /**
     * This method provides a means to explicitly supply configuration to a worker instance.
     * @param configuration worker configuration
     */
    void setConfiguration(WorkerConfiguration configuration);


    /**
     * Start the work on a task.
     * @return the result of the worker operation, and appropriate result data
     * @throws InterruptedException indicates that the task is being aborted as requested by the framework
     * @throws TaskRejectedException indicates this Worker wishes to abandon this task and defer its execution
     * @throws TaskFailedException if the Worker fails in an unrecoverable fashion
     */
    WorkerResponse doWork()
        throws InterruptedException, TaskRejectedException;


    /**
     * @return a string to uniquely identify the sort of tasks this worker will do
     */
    String getWorkerIdentifier();


    /**
     * This should return a number that identifies the API version that this worker uses, and should
     * be incremented when the format of the task data (or result data) changes. Internal code-logic
     * changes should not affect the API version.
     * @return a numeral that identifies the API version of the worker
     */
    int getWorkerApiVersion();


    /**
     * This should return a string that identifies the build version of the worker. Internal code-logic
     * changes will affect the Worker Version.
     * @return a string that identifies the build version of the worker
     */
    String getWorkerVersion();


    /**
     * In case of a Worker's doWork() method failing with an unhandled exception, it is expected a
     * Worker should be able to return a general result.
     * @param t the throwable that caused the unhandled Worker failure
     * @return a response in case of a general unhandled exception failure scenario
     */
    WorkerResponse getGeneralFailureResult(Throwable t);
}
