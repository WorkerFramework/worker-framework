package com.hpe.caf.worker.core;

import com.hpe.caf.api.worker.TaskRejectedException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class WorkerThreadPool {

    private final BlockingQueue<Runnable> workQueue;
    private final PrivateWorkerThreadPoolExecutor threadPoolExecutor;

    public WorkerThreadPool(final int nThreads) {
        workQueue = new LinkedBlockingQueue<>();
        threadPoolExecutor = new PrivateWorkerThreadPoolExecutor(nThreads, workQueue);
    }

    public void shutdown() {
        threadPoolExecutor.shutdown();
    }

    public void awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException
    {
        threadPoolExecutor.awaitTermination(timeout, unit);
    }

    public int getActiveCount() {
        return threadPoolExecutor.getActiveCount();
    }

    public int getBacklogSize() {
        return workQueue.size();
    }

    /**
     * Pass off a runnable task to the backend, considering a hard upper bound to the internal backlog.
     * @param wrapper the new task to run
     * @param id a unique task id
     * @throws TaskRejectedException if no more tasks can be added to the internal backlog
     */
    public void submit(final Runnable wrapper, final String id)
        throws TaskRejectedException
    {
        threadPoolExecutor.submit(wrapper, id);
    }

    public int abortTasks() {
        return threadPoolExecutor.abortTasks();
    }

    private static class PrivateWorkerThreadPoolExecutor extends WorkerThreadPoolExecutor {

        private final Map<RunnableFuture<?>, Runnable> tasks;

        public PrivateWorkerThreadPoolExecutor(final int nThreads, final BlockingQueue<Runnable> workQueue) {
            super(nThreads,
                  nThreads,
                  0L,
                  TimeUnit.MILLISECONDS,
                  workQueue,
                  () -> System.exit(1));
            
            tasks = new ConcurrentHashMap<>();
        }

        @Override
        public void afterExecute(Runnable r, Throwable t) {
            try {
                super.afterExecute(r, t);
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
