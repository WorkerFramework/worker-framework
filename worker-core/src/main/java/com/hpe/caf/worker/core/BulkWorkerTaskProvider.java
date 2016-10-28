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
