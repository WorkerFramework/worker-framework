/*
 * Copyright 2015-2024 Open Text.
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
package com.opentext.caf.api.worker;

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
}
