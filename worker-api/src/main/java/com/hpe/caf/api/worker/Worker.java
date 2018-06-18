/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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

/**
 * A Worker can be constructed in any way as per suits the developer, but should only perform the bare minimum of tasks in the constructor
 * to set itself up to perform the computational work. At some point after construction, the worker-core framework will call through to
 * doWork(), at which point this Worker will be on its own separately managed thread and can start performing useful operations. If the
 * Worker throws an exception from the constructor, this task will be rejected back onto the queue (and eventually it may be dropped,
 * depending upon the WorkerQueue implementation).
 *
 * There are no limits upon time taken for the Worker to perform its task, but it must at some point terminate either via throwing an
 * exception returning from doWork() by returning a WorkerResponse object. The Worker base class has various utility methods for returning
 * a WorkerResponse, such as createSuccessResult, createFailureResult, and createTaskSubmission. Preferably a Worker will always return
 * one of these as opposed to throwing a WorkerException out of the object.
 *
 * Finally, a Worker has methods to classify the type of work it is performing (an "identifier") and another method that returns the
 * integer API version of the task data. These are typically defined in your shareed package that contains the task and result classes,
 * but are used here for constructing a WorkerResponse.
 */
public interface Worker
{
    /**
     * Start the work on a task.
     *
     * @return the result of the worker operation, and appropriate result data
     * @throws InterruptedException indicates that the task is being aborted as requested by the framework
     * @throws TaskRejectedException indicates this Worker wishes to abandon this task and defer its execution
     * @throws TaskFailedException if the Worker fails in an unrecoverable fashion
     * @throws InvalidTaskException if the Worker fails to understand the task to process
     */
    WorkerResponse doWork()
        throws InterruptedException, TaskRejectedException, InvalidTaskException;

    /**
     * @return a string to uniquely identify the sort of tasks this worker will do
     */
    String getWorkerIdentifier();

    /**
     * This should return a number that identifies the API version that this worker uses, and should be incremented when the format of the
     * task data (or result data) changes. Internal code-logic changes should not affect the API version.
     *
     * @return a numeral that identifies the API version of the worker
     */
    int getWorkerApiVersion();

    /**
     * In case of a Worker's doWork() method failing with an unhandled exception, it is expected a Worker should be able to return a
     * general result.
     *
     * @param t the throwable that caused the unhandled Worker failure
     * @return a response in case of a general unhandled exception failure scenario
     */
    WorkerResponse getGeneralFailureResult(Throwable t);
}
