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

import com.codahale.metrics.Timer;
import com.hpe.caf.api.worker.*;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper for a worker used internally by the worker core. It is a Runnable that executes a worker but ensures that a result is always
 * returned, even if the task throws some unhandled exception. The wrapper will use a CompleteTaskCallback once the worker has terminated.
 */
class StreamingWorkerWrapper implements Runnable
{
    private final Worker worker;
    private final WorkerTaskImpl workerTask;
    private static final Timer TIMER = new Timer();
    private static final Logger LOG = LoggerFactory.getLogger(StreamingWorkerWrapper.class);

    public StreamingWorkerWrapper(final WorkerTaskImpl workerTask)
        throws InvalidTaskException, TaskRejectedException
    {
        this.worker = workerTask.createWorker();
        this.workerTask = workerTask;
    }

    /**
     * Trigger the worker to perform its necessary computations and handle the result. The wrapper will ensure some manner of result
     * returns, whether the worker succeeds, fails explicitly, or otherwise. However, if the Thread is interrupted (signalling the Worker
     * is being cancelled entirely) no callback is performed.
     */
    @Override
    public void run()
    {
        try {
            if (workerTask.isPoison()) {
                LOG.warn("Worker [" + worker.getWorkerIdentifier() + "] did not handle poisoned message, when it was passed for processing.");
                sendPoisonMessage();
                throw new RuntimeException("Worker [" + worker.getWorkerIdentifier() + "] did not handle poisoned message, when it was passed for processing.");
            } else {
                Timer.Context t = TIMER.time();
                WorkerResponse response = worker.doWork();
                t.stop();
                workerTask.setResponse(response);
            }
        } catch (TaskRejectedException e) {
            workerTask.setResponse(e);
        } catch (InvalidTaskException e) {
            workerTask.setResponse(e);
        } catch (InterruptedException e) {
            workerTask.logInterruptedException(e);
            workerTask.setResponse(new TaskRejectedException("Worker ["+ worker.getWorkerIdentifier()+"] was interrupted.", e));
        } catch (RuntimeException e) {
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

    private void sendPoisonMessage()
    {
        // Publish poison message to "reject" queue
        final TaskMessage poisonMessage = new TaskMessage(
                "",
                "",
                workerTask.getVersion(),
                workerTask.getData(),
                TaskStatus.RESULT_EXCEPTION,
                new HashMap<>(),
                workerTask.getRejectQueue(),
                workerTask.getTrackingInfo(),
                workerTask.getSourceInfo());
        workerTask.sendMessage(poisonMessage);
    }
}
