/*
 * Copyright 2015-2023 Open Text.
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
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.hpe.caf.api.worker.*;

import java.util.Collections;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * A wrapper for a worker used internally by the worker core. It is a Runnable that executes a worker but ensures that a result is always
 * returned, even if the task throws some unhandled exception. The wrapper will use a CompleteTaskCallback once the worker has terminated.
 */
class StreamingWorkerWrapper implements Runnable
{
    private static final String CORRELATION_ID = "correlationId";
    private static final String CAF_WORKER_NAME = "CAF_WORKER_NAME";
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
        final String workerFriendlyName = !Strings.isNullOrEmpty(System.getenv(CAF_WORKER_NAME)) ?
                System.getenv(CAF_WORKER_NAME) : worker.getClass().getName();
        try {
            if (workerTask.isPoison()) {
                sendPoisonMessage();
                throw new RuntimeException(workerFriendlyName + " could not process the document.");
            } else {
                Timer.Context t = TIMER.time();
                MDC.put(CORRELATION_ID, workerTask.getCorrelationId());
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
            workerTask.setResponse(new TaskRejectedException("["+ workerFriendlyName + "] was interrupted.", e));
        } catch (RuntimeException e) {
            LOG.warn("Worker threw unhandled exception", e);
            workerTask.setResponse(worker.getGeneralFailureResult(e));
        }
        finally {
            MDC.remove(CORRELATION_ID);
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
                UUID.randomUUID().toString(),
                MoreObjects.firstNonNull(workerTask.getClassifier(), ""),
                workerTask.getVersion(),
                workerTask.getData(),
                TaskStatus.RESULT_EXCEPTION,
                Collections.<String, byte[]>emptyMap(),
                workerTask.getRejectQueue(),
                workerTask.getTrackingInfo(),
                workerTask.getSourceInfo(),
                workerTask.getCorrelationId());
        workerTask.sendMessage(poisonMessage);
    }
}
