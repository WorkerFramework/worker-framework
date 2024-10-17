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
 * This object is supplied to CAF Workers which are able to process multiple tasks together. It allows them to request additional tasks.
 */
public interface BulkWorkerRuntime
{
    /**
     * Retrieves another task to be processed. This method does not block; if a task is not readily available then null is returned
     * immediately.
     *
     * @return the next task to be processed or null if a task is not readily available
     */
    WorkerTask getNextWorkerTask();

    /**
     * Retrieves another task to be processed. If a task is not readily available then this method will block waiting for a task for the
     * specified number of milliseconds, after which it will return null if a task is still not available.
     *
     * @param millis the maximum number of milliseconds to wait on a task
     * @return the next task to be processed or null if a task is not available
     * @throws InterruptedException if the thread is interrupted while blocking
     */
    WorkerTask getNextWorkerTask(long millis) throws InterruptedException;
}
