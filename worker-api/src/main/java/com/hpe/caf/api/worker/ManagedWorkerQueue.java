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
package com.hpe.caf.api.worker;


import com.hpe.caf.api.HealthReporter;

/**
 * A WorkerQueue for use at an application level, supporting management methods.
 */
public interface ManagedWorkerQueue extends HealthReporter, WorkerQueue
{
    /**
     * Open queues to start accepting tasks and results.
     * @param callback the callback to use when registering or aborting tasks
     * @throws QueueException if the queue cannot be started
     */
    void start(TaskCallback callback)
        throws QueueException;


    /**
     * Halt the incoming queue so that no more tasks are picked up.
     */
    void shutdownIncoming();


    /**
     * Terminate all queue operations.
     */
    void shutdown();


    /**
     * @return the metrics implementation for this WorkerQueue
     */
    WorkerQueueMetricsReporter getMetrics();
    
    /**
     * Disconnects the incoming queue so that no more tasks are consumed
     */
    void disconnectIncoming();
    
    /**
     * Reconnects the incoming queue so tasks consumption can resume
     */
    void reconnectIncoming();
}
