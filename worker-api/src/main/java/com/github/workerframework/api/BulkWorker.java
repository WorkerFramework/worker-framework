/*
 * Copyright 2015-2025 Open Text.
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
package com.github.workerframework.api;

import java.nio.charset.StandardCharsets;

/**
 * This interface should be implemented by CAF Workers which are able to process multiple tasks together.
 *
 * It is expected to be implemented by the WorkerFactory object which has been supplied by the getWorkerFactory() method of the
 * WorkerFactoryProvider class.
 */
public interface BulkWorker
{
    /**
     * The Worker should begin processing the tasks. It can use the runtime object to retrieve the tasks.
     *
     * @param runtime is used to retrieve the tasks
     * @throws InterruptedException if the thread is interrupted by another thread
     */
    void processTasks(BulkWorkerRuntime runtime)
        throws InterruptedException;

    /**
     * If a message has been identified as a poison message, prepare a WorkerResponse that includes the friendly name
     * of the worker.
     * For compatibility with existing Worker implementations a default implementation has been provided.
     *
     * @param workerFriendlyName the worker's friendly name
     * @return a response containing details of the worker that encountered a poison message
     */
    default WorkerResponse getPoisonMessageResult(String workerFriendlyName, final WorkerTask workerTask) {
        final String strData = workerFriendlyName + " could not process the item.";
        final byte[] byteArrayData = strData.getBytes(StandardCharsets.UTF_8);

        // TODO
        // In Abstract Worker we have:
        // return new WorkerResponse(getResultQueue(), TaskStatus.RESULT_EXCEPTION, getExceptionData(t), getWorkerIdentifier(), getWorkerApiVersion(), null);
        //
        // I don't know what the equivalent of getResultQueue(), getWorkerIdentifier() and getWorkerApiVersion() are here,
        // or where we could ge them from in the BulkWorker?
        return new WorkerResponse(
            workerTask.getTo(), TaskStatus.RESULT_EXCEPTION, byteArrayData, "", workerTask.getVersion(), null);
    }
}
