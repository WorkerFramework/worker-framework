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

import com.hpe.caf.api.worker.BulkWorker;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.WorkerFactory;
import java.util.concurrent.TimeUnit;

interface WorkerThreadPool
{
    /* private */ static final Runnable defaultHandler = () -> System.exit(1);

    static WorkerThreadPool create(final int nThreads)
    {
        return create(nThreads, defaultHandler);
    }

    static WorkerThreadPool create(final WorkerFactory workerFactory)
    {
        return create(workerFactory, defaultHandler);
    }

    static WorkerThreadPool create(final WorkerFactory workerFactory, final Runnable handler)
    {
        if (workerFactory instanceof BulkWorker) {
            return new BulkWorkerThreadPool(workerFactory, handler);
        } else {
            return create(workerFactory.getWorkerThreads(), handler);
        }
    }

    static WorkerThreadPool create(final int nThreads, final Runnable handler)
    {
        return new StreamingWorkerThreadPool(nThreads, handler);
    }

    void shutdown();

    void awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Returns whether or not any threads are active
     *
     * @return true if there are no active threads
     */
    boolean isIdle();

    int getBacklogSize();

    /**
     * Execute the specified task at some point in the future
     *
     * @param workerTask the task to be run
     * @throws TaskRejectedException if no more tasks can be accepted
     */
    void submitWorkerTask(WorkerTaskImpl workerTask)
        throws TaskRejectedException;

    int abortTasks();
}
