/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Container for all statistics provided by WorkerCore.
 */
class WorkerStats
{
    private final AtomicLong tasksReceived = new AtomicLong(0);
    private final AtomicLong tasksRejected = new AtomicLong(0);
    private final AtomicLong tasksSucceeded = new AtomicLong(0);
    private final AtomicLong tasksFailed = new AtomicLong(0);
    private final AtomicLong tasksAborted = new AtomicLong(0);
    private final AtomicLong tasksForwarded = new AtomicLong(0);
    private final AtomicLong tasksPaused = new AtomicLong(0);
    private final AtomicLong tasksDiscarded = new AtomicLong(0);
    private final AtomicLong lastTaskFinished = new AtomicLong(System.currentTimeMillis());
    private final Histogram inputSizes = new Histogram(new ExponentiallyDecayingReservoir());
    private final Histogram outputSizes = new Histogram(new ExponentiallyDecayingReservoir());

    /**
     * @return the number of tasks that a WorkerQueue has handed off to WorkerCore
     */
    public long getTasksReceived()
    {
        return tasksReceived.get();
    }

    public void incrementTasksReceived()
    {
        tasksReceived.incrementAndGet();
    }

    /**
     * @return the number of tasks WorkerCore has rejected back to a WorkerQueue
     */
    public long getTasksRejected()
    {
        return tasksRejected.get();
    }

    public void incrementTasksRejected()
    {
        tasksRejected.incrementAndGet();
    }

    /**
     * @return the number of completed tasks returned to WorkerCore that were marked successful by a Worker
     */
    public long getTasksSucceeded()
    {
        return tasksSucceeded.get();
    }

    public void incrementTasksSucceeded()
    {
        tasksSucceeded.incrementAndGet();
    }

    /**
     * @return the number of completed tasks returned to WorkerCore that were marked failed by a Worker
     */
    public long getTasksFailed()
    {
        return tasksFailed.get();
    }

    public void incrementTasksFailed()
    {
        tasksFailed.incrementAndGet();
    }

    /**
     * @return the number of tasks that were aborted (but requeued) due to connection errors or other abnormalities
     */
    public long getTasksAborted()
    {
        return tasksAborted.get();
    }

    public void incrementTasksAborted()
    {
        tasksAborted.incrementAndGet();
    }

    public void incrementTasksAborted(long delta)
    {
        tasksAborted.addAndGet(delta);
    }

    /**
     * @return the number of tasks WorkerCore has forwarded to a WorkerQueue
     */
    public long getTasksForwarded()
    {
        return tasksForwarded.get();
    }

    public void incrementTasksForwarded()
    {
        tasksForwarded.incrementAndGet();
    }

    /**
     * @return the number of tasks WorkerCore has forwarded to a WorkerQueue designated for paused tasks
     */
    public long getTasksPaused()
    {
        return tasksPaused.get();
    }

    public void incrementTasksPaused()
    {
        tasksPaused.incrementAndGet();
    }

    /**
     * @return the number of tasks that were discarded (without being requeued)
     */
    public long getTasksDiscarded()
    {
        return tasksDiscarded.get();
    }

    public void incrementTasksDiscarded()
    {
        tasksDiscarded.incrementAndGet();
    }

    /**
     * @return the time (in milliseconds) the most recent task completed (or the startup time, if no task has been done yet)
     */
    public long getLastTaskFinishedTime()
    {
        return lastTaskFinished.get();
    }

    public void updatedLastTaskFinishedTime()
    {
        lastTaskFinished.set(System.currentTimeMillis());
    }

    public Histogram getInputSizes()
    {
        return inputSizes;
    }

    public Histogram getOutputSizes()
    {
        return outputSizes;
    }
}
