/*
 * Copyright 2015-2025 Open Text.
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
package com.github.workerframework.core;

import com.github.workerframework.api.BulkWorker;
import com.github.workerframework.api.BulkWorkerRuntime;
import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.api.TaskStatus;
import com.github.workerframework.api.WorkerTask;
import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BulkWorkerTaskProvider implements BulkWorkerRuntime
{
    private static final Logger LOG = LoggerFactory.getLogger(BulkWorkerTaskProvider.class);
    private WorkerTaskImpl firstTask;
    private final BlockingQueue<WorkerTaskImpl> workQueue;
    private final ArrayList<WorkerTaskImpl> consumedTasks;
    private final BulkWorker bulkWorker;
    private final String bulkWorkerFriendlyName;

    public BulkWorkerTaskProvider(
        final WorkerTaskImpl firstTask,
        final BlockingQueue<WorkerTaskImpl> workQueue,
        final BulkWorker bulkWorker,
        final String bulkWorkerFriendlyName)
    {
        this.firstTask = Objects.requireNonNull(firstTask);
        this.workQueue = Objects.requireNonNull(workQueue);
        this.bulkWorker = Objects.requireNonNull(bulkWorker);
        this.bulkWorkerFriendlyName = Objects.requireNonNull(bulkWorkerFriendlyName);
        this.consumedTasks = new ArrayList<>();
    }

    @Override
    public WorkerTask getNextWorkerTask()
    {
        try {
            return getNextWorkerTaskInternal(null);
        } catch (final InterruptedException e) {
            // Should never happen - no-arg version doesn't block
            throw new RuntimeException(e);
        }
    }

    @Override
    public WorkerTask getNextWorkerTask(long millis) throws InterruptedException
    {
        return getNextWorkerTaskInternal(millis);
    }

    private WorkerTask getNextWorkerTaskInternal(final Long millis) throws InterruptedException
    {
        final WorkerTaskImpl workerTask = registerTaskConsumed(
            millis == null ? getNextWorkerTaskImpl() : getNextWorkerTaskImpl(millis)
        );

        if (workerTask != null && workerTask.isPoison()) {
            LOG.info("Received poison message, generating poison response for worker: {}", bulkWorkerFriendlyName);
            workerTask.setResponse(bulkWorker.getPoisonMessageResult(bulkWorkerFriendlyName, workerTask));
            sendCopyToReject(workerTask);
            return getNextWorkerTaskInternal(millis);
        }

        return workerTask;
    }

    private WorkerTaskImpl getNextWorkerTaskImpl()
    {
        final WorkerTaskImpl task = firstTask;
        if (task == null) {
            return workQueue.poll();
        } else {
            firstTask = null;
            return task;
        }
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

    public boolean isFirstTaskConsumed()
    {
        return firstTask == null;
    }

    public Iterable<WorkerTaskImpl> getConsumedTasks()
    {
        return Collections.unmodifiableList(consumedTasks);
    }

    private WorkerTaskImpl registerTaskConsumed(WorkerTaskImpl workerTask)
    {
        if (workerTask != null) {
            consumedTasks.add(workerTask);
        }
        return workerTask;
    }

    private void sendCopyToReject(final WorkerTaskImpl workerTask) {
        final TaskMessage poisonMessage = new TaskMessage(
            UUID.randomUUID().toString(),
            MoreObjects.firstNonNull(workerTask.getClassifier(), ""),
            workerTask.getVersion(),
            workerTask.getData(),
            TaskStatus.RESULT_EXCEPTION,
            Collections.emptyMap(),
            workerTask.getRejectQueue(),
            workerTask.getTrackingInfo(),
            workerTask.getSourceInfo(),
            workerTask.getCorrelationId());

        LOG.info("Sending poison message to: {}",  workerTask.getRejectQueue());

        workerTask.sendMessage(poisonMessage);
    }
}
