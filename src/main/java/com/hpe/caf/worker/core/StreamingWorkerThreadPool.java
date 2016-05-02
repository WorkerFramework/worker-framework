package com.hpe.caf.worker.core;

import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskRejectedException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StreamingWorkerThreadPool implements WorkerThreadPool {

    private final BlockingQueue<Runnable> workQueue;
    private final PrivateWorkerThreadPoolExecutor threadPoolExecutor;

    public StreamingWorkerThreadPool(final int nThreads, final Runnable handler) {
        this(nThreads, handler, true);
    }

    public StreamingWorkerThreadPool
    (
        final int nThreads,
        final Runnable handler,
        final boolean doSizeCheck
    ) {
        workQueue = new LinkedBlockingQueue<>();
        threadPoolExecutor = new PrivateWorkerThreadPoolExecutor(
            nThreads, workQueue, handler, doSizeCheck);
    }

    @Override
    public void shutdown() {
        threadPoolExecutor.shutdown();
    }

    @Override
    public void awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException
    {
        threadPoolExecutor.awaitTermination(timeout, unit);
    }

    /**
     * Returns whether or not any threads are active
     * @return true if there are no active threads
     */
    @Override
    public boolean isIdle() {
        return threadPoolExecutor.getActiveCount() == 0;
    }

    @Override
    public int getBacklogSize() {
        return workQueue.size();
    }

    /**
     * Execute the specified task at some point in the future
     * @param workerTask the task to be run
     * @throws TaskRejectedException if no more tasks can be accepted
     */
    @Override
    public void submitWorkerTask(final WorkerTaskImpl workerTask)
        throws TaskRejectedException
    {
        try {
            StreamingWorkerWrapper wrapper = new StreamingWorkerWrapper(workerTask);
            threadPoolExecutor.submitWithSizeCheck(wrapper);
        } catch (InvalidTaskException e) {
            workerTask.setResponse(e);
        }
    }

    @Override
    public int abortTasks() {
        return threadPoolExecutor.abortTasks();
    }

    private static class PrivateWorkerThreadPoolExecutor extends ThreadPoolExecutor {

        private static final Logger LOG = LoggerFactory.getLogger(PrivateWorkerThreadPoolExecutor.class);

        private final Runnable throwableHandler;
        private final Map<RunnableFuture<?>, Runnable> tasks;
        private final boolean ignoreSizeCheck;

        public PrivateWorkerThreadPoolExecutor
        (
            final int nThreads,
            final BlockingQueue<Runnable> workQueue,
            final Runnable handler,
            final boolean doSizeCheck
        ) {
            super(nThreads,
                  nThreads,
                  0L,
                  TimeUnit.MILLISECONDS,
                  workQueue);

            throwableHandler = Objects.requireNonNull(handler);
            tasks = new ConcurrentHashMap<>();
            ignoreSizeCheck = !doSizeCheck;
        }

        @Override
        public void afterExecute(Runnable r, Throwable t) {
            try {
                super.afterExecute(r, t);
                if ( t != null ) {
                    LOG.error("Worker thread terminated with unhandled throwable, terminating service", t);
                    throwableHandler.run();
                }
            }
            finally {
                tasks.remove(r);
            }
        }

        public void submitWithSizeCheck(final Runnable task)
            throws TaskRejectedException
        {
            if (ignoreSizeCheck || (getQueue().size() < getCorePoolSize() * 10)) {
                submit(task);
            } else {
                throw new TaskRejectedException("Maximum internal task backlog exceeded");
            }
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
            RunnableFuture<T> runnableFuture = super.newTaskFor(runnable, value);
            tasks.put(runnableFuture, runnable);
            return runnableFuture;
        }

        public int abortTasks() {
            AtomicInteger count = new AtomicInteger();

            tasks.forEach((key, value) -> {
                key.cancel(true);
                count.incrementAndGet();
            });
            tasks.clear();

            return count.get();
        }
    }
}
