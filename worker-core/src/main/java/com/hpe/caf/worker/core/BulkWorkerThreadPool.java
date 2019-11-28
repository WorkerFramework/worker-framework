/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
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
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BulkWorkerThreadPool implements WorkerThreadPool
{
    private static final Logger LOG = LoggerFactory.getLogger(BulkWorkerThreadPool.class);

    private final BulkWorker bulkWorker;
    private final BlockingQueue<WorkerTaskImpl> workQueue;
    private final BulkWorkerThread[] bulkWorkerThreads;
    private final Runnable throwableHandler;
    private final StreamingWorkerThreadPool backupThreadPool;

    private volatile boolean isActive;

    public BulkWorkerThreadPool(
        final WorkerFactory workerFactory,
        final Runnable handler
    )
    {
        final int nThreads = workerFactory.getWorkerThreads();

        this.bulkWorker = (BulkWorker) workerFactory;
        this.workQueue = new LinkedBlockingQueue<>();
        this.bulkWorkerThreads = new BulkWorkerThread[nThreads];
        this.throwableHandler = handler;
        this.backupThreadPool = new StreamingWorkerThreadPool(1, handler);
        this.isActive = true;

        for (int i = 0; i < nThreads; i++) {
            BulkWorkerThread bulkWorkerThread = new BulkWorkerThread();
            bulkWorkerThreads[i] = bulkWorkerThread;
            bulkWorkerThread.start();
        }
    }

    private final class BulkWorkerThread extends Thread
    {
        @Override
        public void run()
        {
            try {
                while (isActive) {
                    try {
                        execute();
                    } catch (final InterruptedException ex) {
                        LOG.warn("Bulk Worker interrupt signalled; not performing callbacks", ex);
                    }
                }
            } catch (final Throwable t) {
                LOG.error("Bulk Worker thread terminated with unhandled throwable, terminating service", t);
                throwableHandler.run();
                throw t;
            }
        }

        private void execute()
            throws InterruptedException
        {
            final WorkerTaskImpl task = workQueue.take();
            final BulkWorkerTaskProvider taskProvider
                = new BulkWorkerTaskProvider(task, workQueue);

            try {
                bulkWorker.processTasks(taskProvider);
            } catch (final RuntimeException ex) {
                LOG.warn("Bulk Worker threw unhandled exception", ex);
            } finally {
                // Re-submit the first task if it has not been consumed
                // NB: It's really faulty Worker logic to not consume at least
                // the one task.
                if (!taskProvider.isFirstTaskConsumed()) {
                    LOG.warn("Bulk Worker did not consume even the first task; "
                        + "re-submitting it...");
                    resubmitWorkerTask(task);
                }

                // Re-submit any consumed tasks that have not been responded to
                resubmitIgnoredWorkerTasks(taskProvider);
            }
        }
    }

    @Override
    public void shutdown()
    {
        isActive = false;

        for (BulkWorkerThread workerThread : bulkWorkerThreads) {
            workerThread.interrupt();
        }

        backupThreadPool.shutdown();
    }

    @Override
    public void awaitTermination(final long timeout, final TimeUnit unit)
        throws InterruptedException
    {
        final long timeoutMillis
            = System.currentTimeMillis() + unit.toMillis(timeout);

        backupThreadPool.awaitTermination(timeout, unit);

        for (BulkWorkerThread workerThread : bulkWorkerThreads) {
            final long timeLeft = timeoutMillis - System.currentTimeMillis();
            if (timeLeft <= 0) {
                return;
            }

            workerThread.join(timeLeft);
        }
    }

    @Override
    public boolean isIdle()
    {
        // This implementation is not perfect, as a final task could have just
        // been removed from the queue, and so a thread could be working on it,
        // but it is probably close enough (as it's only used for metrics)
        return workQueue.isEmpty() && backupThreadPool.isIdle();
    }

    @Override
    public int getBacklogSize()
    {
        return workQueue.size() + backupThreadPool.getBacklogSize();
    }

    @Override
    public void submitWorkerTask(final WorkerTaskImpl workerTask)
        throws TaskRejectedException
    {
        if (!workQueue.offer(workerTask)) {
            throw new TaskRejectedException(
                "Bulk Worker: Maximum internal task backlog exceeded");
        }
    }

    @Override
    public int abortTasks()
    {
        final int backgroundAbortTaskCount = backupThreadPool.abortTasks();

        final ArrayList<WorkerTaskImpl> tasksToBeAborted
            = new ArrayList<>(workQueue.size() + 16);

        return workQueue.drainTo(tasksToBeAborted) + backgroundAbortTaskCount;
    }

    /**
     * Checks that all of the tasks that have been consumed by the Bulk Worker have been responded to, and re-submits any that have not
     * been responded to (using the traditional non-bulk interface).
     */
    private void resubmitIgnoredWorkerTasks(
        final BulkWorkerTaskProvider taskProvider
    )
    {
        for (WorkerTaskImpl task : taskProvider.getConsumedTasks()) {
            if (!task.isResponseSet()) {
                LOG.warn("Bulk Worker Framework re-submitting a task whose "
                    + "response was not set...");
                resubmitWorkerTask(task);
            }
        }
    }

    /**
     * Submits the specified task to the backup thread pool, where is will be processed using the traditional one-by-one Worker
     * interfaces.
     */
    private void resubmitWorkerTask(final WorkerTaskImpl workerTask)
    {
        // Assert that the worker task has not been responded to
        assert workerTask != null;
        assert !workerTask.isResponseSet();

        // Try to submit the task to the backup thread pool
        try {
            backupThreadPool.submitWorkerTask(workerTask);
        } catch (TaskRejectedException ex) {
            workerTask.setResponse(ex);
        }
    }
}
