/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.naming.ServicePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Utility class for preparing a new Worker for a task and executing it.
 */
final class WorkerExecutor
{
    private final ServicePath servicePath;
    private final WorkerCallback callback;
    private final WorkerFactory factory;
    private final WorkerThreadPool threadPool;
    private final MessagePriorityManager priorityManager;
    private static final Logger LOG = LoggerFactory.getLogger(WorkerExecutor.class);

    /**
     * Create a WorkerWrapperFactory. The constructor parameters are the fixed properties of every WorkerWrapper on this micro-service
     * worker.
     *
     * @param path the service path of this worker service
     * @param callback the callback the wrappers use when a task completes
     * @param workerFactory the origin of the Worker objects themselves
     */
    public WorkerExecutor(
        final ServicePath path,
        final WorkerCallback callback,
        final WorkerFactory workerFactory,
        final WorkerThreadPool pool,
        final MessagePriorityManager priorityManager
    )
    {
        this.servicePath = Objects.requireNonNull(path);
        this.callback = Objects.requireNonNull(callback);
        this.factory = Objects.requireNonNull(workerFactory);
        this.threadPool = Objects.requireNonNull(pool);
        this.priorityManager = Objects.requireNonNull(priorityManager);
    }

    /**
     * Get a new Worker for a TaskMessage and hand the Worker off to a thread pool to execute, wrapped in a WorkerWrapper. If the
     * WorkerFactory indicates the task-specific data is invalid, a response is immediately returned indicating this.
     *
     * @param tm the task message
     * @param taskInformation the reference to the message this task arrived on
     * @param headers the map of key/value paired headers to be stamped on the message
     * @throws TaskRejectedException if the WorkerFactory indicates the task cannot be handled at this time
     */
    public void executeTask(final TaskMessage tm, final TaskInformation taskInformation, final boolean poison,
                            final Map<String, Object> headers, final Codec codec)
        throws TaskRejectedException
    {
        final WorkerTaskImpl workerTask = createWorkerTask(taskInformation, tm, poison, headers, codec);

        threadPool.submitWorkerTask(workerTask);
    }

    /**
     * Decide whether the message is to be forwarded or discarded.
     *
     * @param tm the task message
     * @param taskInformation the reference to the message this task arrived on
     * @param headers the map of key/value paired headers to be stamped on the message
     */
    public void forwardTask(final TaskMessage tm, final TaskInformation taskInformation, Map<String, Object> headers) throws TaskRejectedException
    {
        //Check whether this worker application can evaluate messages for forwarding.
        if (factory instanceof TaskMessageForwardingEvaluator) {
            ((TaskMessageForwardingEvaluator) factory).determineForwardingAction(tm, taskInformation, headers, callback);
        } else {
            //Messages are forwarded by default.
            callback.forward(taskInformation, tm.getTo(), tm, headers);
        }
    }

    /**
     * Forward the supplied task message to the paused queue.
     *
     * @param tm the task message
     * @param taskInformation the reference to the message this task arrived on
     * @param pausedQueue the message to put on the paused queue
     * @param headers the map of key/value paired headers to be stamped on the message
     */
    public void pauseTask(final TaskMessage tm, final TaskInformation taskInformation, final String pausedQueue,
                          final Map<String, Object> headers) throws TaskRejectedException
    {
        callback.pause(taskInformation, tm.getTo(), tm, headers);
    }

    /**
     * Discard the supplied task message.
     *
     * @param tm the task message to be discarded
     * @param taskInformation the reference to the message this task arrived on
     */
    public void discardTask(final TaskMessage tm, final TaskInformation taskInformation) throws TaskRejectedException
    {
        LOG.warn("Discarding task {} (message id: {})", tm.getTaskId(), taskInformation.getInboundMessageId());
        callback.discard(taskInformation);
    }

    /**
     * Creates a WorkerTask for the specified message
     */
    private WorkerTaskImpl createWorkerTask(final TaskInformation taskInformation, final TaskMessage taskMessage, final boolean poison,
                                            final Map<String, Object> headers, final Codec codec)
    {
        return new WorkerTaskImpl(servicePath, callback, factory, taskInformation, taskMessage, poison, headers, codec,
                priorityManager);
    }
}
