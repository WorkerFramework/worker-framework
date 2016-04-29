package com.hpe.caf.worker.core;

import com.codahale.metrics.Timer;
import com.hpe.caf.api.worker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper for a worker used internally by the worker core. It is a Runnable that
 * executes a worker but ensures that a result is always returned, even if the task
 * throws some unhandled exception. The wrapper will use a CompleteTaskCallback once
 * the worker has terminated.
 */
class WorkerWrapper implements Runnable
{
    private final Worker worker;
    private final WorkerTaskImpl workerTask;
    private static final Timer TIMER = new Timer();
    private static final Logger LOG = LoggerFactory.getLogger(WorkerWrapper.class);


    public WorkerWrapper(final WorkerTaskImpl workerTask)
        throws InvalidTaskException, TaskRejectedException
    {
        this.worker = workerTask.createWorker();
        this.workerTask = workerTask;
    }


    /**
     * Trigger the worker to perform its necessary computations and handle the result. The wrapper will
     * ensure some manner of result returns, whether the worker succeeds, fails explicitly, or otherwise.
     * However, if the Thread is interrupted (signalling the Worker is being cancelled entirely) no callback
     * is performed.
     */
    @Override
    public void run()
    {
        try {
            Timer.Context t = TIMER.time();
            WorkerResponse response = worker.doWork();
            t.stop();
            workerTask.setResponse(response);
        } catch (TaskRejectedException e) {
            workerTask.setResponse(e);
        } catch (InterruptedException e) {
            workerTask.logInterruptedException(e);
        } catch (Exception e) {
            LOG.warn("Worker threw unhandled exception", e);
            workerTask.setResponse(worker.getGeneralFailureResult(e));
        }
    }


    /**
     * @return the timer used for keeping statistics on worker run times
     */
    public static Timer getTimer()
    {
        return TIMER;
    }
}
