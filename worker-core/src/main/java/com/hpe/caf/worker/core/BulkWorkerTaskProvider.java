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

import com.hpe.caf.api.worker.BulkWorkerRuntime;
import com.hpe.caf.api.worker.WorkerTask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

final class BulkWorkerTaskProvider implements BulkWorkerRuntime
{
    private WorkerTaskImpl firstTask;
    private final BlockingQueue<WorkerTaskImpl> workQueue;
    private final ArrayList<WorkerTaskImpl> consumedTasks;

    public BulkWorkerTaskProvider
    (
        final WorkerTaskImpl firstTask,
        final BlockingQueue<WorkerTaskImpl> workQueue
    ) {
        this.firstTask = Objects.requireNonNull(firstTask);
        this.workQueue = Objects.requireNonNull(workQueue);
        this.consumedTasks = new ArrayList<>();
    }

    @Override
    public WorkerTask getNextWorkerTask() {
        return registerTaskConsumed(getNextWorkerTaskImpl());
    }

    private WorkerTaskImpl getNextWorkerTaskImpl() {
        final WorkerTaskImpl task = firstTask;
        if (task == null) {
            return workQueue.poll();
        } else {
            firstTask = null;
            return task;
        }
    }

    @Override
    public WorkerTask getNextWorkerTask(long millis) throws InterruptedException {
        return registerTaskConsumed(getNextWorkerTaskImpl(millis));
    }

    private WorkerTaskImpl getNextWorkerTaskImpl(long millis)
        throws InterruptedException
    {
        final WorkerTaskImpl task = firstTask;
        if (task == null) {
            return workQueue.poll(millis, TimeUnit.MILLISECONDS);
        } else {
            firstTask = null;
            return task;
        }
    }

    public boolean isFirstTaskConsumed() {
        return firstTask == null;
    }

    public Iterable<WorkerTaskImpl> getConsumedTasks() {
        return Collections.unmodifiableList(consumedTasks);
    }

    private WorkerTaskImpl registerTaskConsumed(WorkerTaskImpl workerTask) {
        if (workerTask != null) {
            consumedTasks.add(workerTask);
        }
        return workerTask;
    }
}
