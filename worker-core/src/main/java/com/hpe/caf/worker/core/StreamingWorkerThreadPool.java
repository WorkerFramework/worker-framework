/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StreamingWorkerThreadPool implements WorkerThreadPool
{
    private final BlockingQueue<Runnable> workQueue;
    private final PrivateWorkerThreadPoolExecutor threadPoolExecutor;

    public StreamingWorkerThreadPool(
        final int nThreads,
        final Runnable handler
    )
    {
        workQueue = new LinkedBlockingQueue<>();
        threadPoolExecutor = new PrivateWorkerThreadPoolExecutor(
            nThreads, workQueue, handler);
    }

    @Override
    public void shutdown()
    {
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
     *
     * @return true if there are no active threads
     */
    @Override
    public boolean isIdle()
    {
        return threadPoolExecutor.getActiveCount() == 0;
    }

    @Override
    public int getBacklogSize()
    {
        return workQueue.size();
    }

    /**
     * Execute the specified task at some point in the future
     *
     * @param workerTask the task to be run
     * @throws TaskRejectedException if no more tasks can be accepted
     */
    @Override
    public void submitWorkerTask(final WorkerTaskImpl workerTask)
        throws TaskRejectedException
    {
        try {
            StreamingWorkerWrapper wrapper = new StreamingWorkerWrapper(workerTask);
            threadPoolExecutor.submit(wrapper);
        } catch (InvalidTaskException e) {
            workerTask.setResponse(e);
        }
    }

    @Override
    public int abortTasks()
    {
        return threadPoolExecutor.abortTasks();
    }

    private static class PrivateWorkerThreadPoolExecutor extends ThreadPoolExecutor
    {

        private static final Logger LOG = LoggerFactory.getLogger(PrivateWorkerThreadPoolExecutor.class);

        private final Runnable throwableHandler;
        private final Map<RunnableFuture<?>, Runnable> tasks;

        public PrivateWorkerThreadPoolExecutor(
            final int nThreads,
            final BlockingQueue<Runnable> workQueue,
            final Runnable handler
        )
        {
            super(nThreads,
                  nThreads,
                  0L,
                  TimeUnit.MILLISECONDS,
                  workQueue);

            throwableHandler = Objects.requireNonNull(handler);
            tasks = new ConcurrentHashMap<>();
        }

        @Override
        public void afterExecute(Runnable r, Throwable t)
        {
            try {
                super.afterExecute(r, t);
                if (t == null && r instanceof Future<?>) {
                    try {
                        ((Future<?>) r).get();
                    } catch (CancellationException ce) {
                        LOG.error("Thread returned a CancellationException with message " + ce.getMessage(), ce);
                    } catch (ExecutionException ee) {
                        t = ee.getCause();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); // ignore/reset
                    }
                }
                if (t != null) {
                    LOG.error("Worker thread terminated with unhandled throwable, terminating service", t);
                    throwableHandler.run();
                }
            } finally {
                tasks.remove(r);
            }
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value)
        {
            RunnableFuture<T> runnableFuture = super.newTaskFor(runnable, value);
            tasks.put(runnableFuture, runnable);
            return runnableFuture;
        }

        public int abortTasks()
        {
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
