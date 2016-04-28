package com.hpe.caf.worker.core;

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

final class WorkerThreadPoolImpl implements WorkerThreadPool {

    private final BlockingQueue<Runnable> workQueue;
    private final PrivateWorkerThreadPoolExecutor threadPoolExecutor;

    public WorkerThreadPoolImpl(final int nThreads, final Runnable handler) {
        workQueue = new LinkedBlockingQueue<>();
        threadPoolExecutor = new PrivateWorkerThreadPoolExecutor(nThreads, workQueue, handler);
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
     * Pass off a runnable task to the backend, considering a hard upper bound to the internal backlog.
     * @param wrapper the new task to run
     * @param id a unique task id
     * @throws TaskRejectedException if no more tasks can be added to the internal backlog
     */
    @Override
    public void submit(final Runnable wrapper, final String id)
        throws TaskRejectedException
    {
        threadPoolExecutor.submit(wrapper, id);
    }

    @Override
    public int abortTasks() {
        return threadPoolExecutor.abortTasks();
    }

    private static class PrivateWorkerThreadPoolExecutor extends ThreadPoolExecutor {

        private static final Logger LOG = LoggerFactory.getLogger(PrivateWorkerThreadPoolExecutor.class);

        private final Runnable throwableHandler;
        private final Map<RunnableFuture<?>, Runnable> tasks;

        public PrivateWorkerThreadPoolExecutor
        (
            final int nThreads,
            final BlockingQueue<Runnable> workQueue,
            final Runnable handler
        ) {
            super(nThreads,
                  nThreads,
                  0L,
                  TimeUnit.MILLISECONDS,
                  workQueue);

            throwableHandler = Objects.requireNonNull(handler);
            tasks = new ConcurrentHashMap<>();
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

        public void submit(final Runnable wrapper, final String id)
            throws TaskRejectedException
        {
            if (getQueue().size() < getCorePoolSize() * 10) {
                //tasks.put(id, threadPoolExecutor.submit(wrapper));
                //threadPoolExecutor.submit(wrapper);
                submit(wrapper);
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
