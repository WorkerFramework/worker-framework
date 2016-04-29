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

    private volatile boolean isActive;

    public BulkWorkerThreadPool
    (
        final WorkerFactory workerFactory,
        final Runnable handler
    ) {
        final int nThreads = workerFactory.getWorkerThreads();

        this.bulkWorker = (BulkWorker) workerFactory;
        this.workQueue = new LinkedBlockingQueue<>(nThreads * 10);
        this.bulkWorkerThreads = new BulkWorkerThread[nThreads];
        this.throwableHandler = handler;
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
        public void run() {
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
            final BulkWorkerTaskProvider taskProvider;
            try {
                taskProvider = new BulkWorkerTaskProvider(task, workQueue);
            } catch (final Throwable t) {
                LOG.warn("Bulk Worker Provider construction failure", t);
                workQueue.put(task);
                throw t;
            }

            try {
                bulkWorker.processTasks(taskProvider);
            }
            finally {
                // We could use a double-ended queue and put it back to the
                // front of the queue, but we'll not worry about this as it's
                // really faulty Worker logic to not collect at least the first
                // task.
                if (!taskProvider.isFirstTaskConsumed()) {
                    LOG.warn("Bulk Worker did not consume the first task; re-queueing it...");
                    workQueue.put(task);
                }
            }

            // Put any consumed messages that have not been acknoledged
            // back on the work queue
            for (WorkerTaskImpl consumedTask : taskProvider.getConsumedTasks()) {
                if (!consumedTask.isResponseSet()) {
                    LOG.warn("Bulk Worker re-queueing a task whose response was not set...");
                    workQueue.put(consumedTask);
                }
            }
        }
    }

    @Override
    public void shutdown() {
        isActive = false;

        for (BulkWorkerThread workerThread : bulkWorkerThreads) {
            workerThread.interrupt();
        }
    }

    @Override
    public void awaitTermination(final long timeout, final TimeUnit unit)
        throws InterruptedException
    {
        final long timeoutMillis =
            System.currentTimeMillis() + unit.toMillis(timeout);

        for (BulkWorkerThread workerThread : bulkWorkerThreads)
        {
            final long timeLeft = timeoutMillis - System.currentTimeMillis();
            if (timeLeft <= 0) {
                return;
            }

            workerThread.join(timeLeft);
        }
    }

    @Override
    public boolean isIdle() {
        // This implementation is not perfect, as a final task could have just
        // been removed from the queue, and so a thread could be working on it,
        // but it is probably close enough (as it's only used for metrics)
        return workQueue.isEmpty();
    }

    @Override
    public int getBacklogSize() {
        return workQueue.size();
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
    public int abortTasks() {
        final ArrayList<WorkerTaskImpl> tasksToBeAborted =
            new ArrayList<>(workQueue.size() + 16);

        return workQueue.drainTo(tasksToBeAborted);
    }
}
