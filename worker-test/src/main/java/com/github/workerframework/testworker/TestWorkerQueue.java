/*
 * Copyright 2015-2024 Open Text.
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
package com.github.workerframework.testworker;

import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.TaskInformation;
import com.hpe.caf.api.worker.WorkerQueueMetricsReporter;

import java.util.Map;

public final class TestWorkerQueue implements ManagedWorkerQueue
{

    public TestWorkerQueue(
            final TestWorkerQueueConfiguration queueCfg
    )
    {
    }

    public void start(final TaskCallback callback) throws QueueException
    {
    }

    public boolean isReceiving()
    {
        return true;
    }

    @Override
    public void publish(
            final TaskInformation taskInformation,
            final byte[] taskMessage,
            final String targetQueue, // DDD Would the target queue ALWAYS be the same?
            final Map<String, Object> headers,
            final boolean isLastMessage // DDD unused ?
    ) throws QueueException
    {
    }

    @Override
    public void publish(
            final TaskInformation taskInformation,
            final byte[] taskMessage,
            final String targetQueue,
            final Map<String, Object> headers) throws QueueException
    {

    }

    /**
     * @param taskInformation The object containing metadata about a queued message.
     */
    @Override
    public void acknowledgeTask(final TaskInformation taskInformation)
    {
    }

    @Override
    public HealthResult livenessCheck()
    {
        return HealthResult.RESULT_HEALTHY;
    }

    @Override
    public HealthResult healthCheck()
    {
        return livenessCheck();
    }

    @Override
    public void shutdownIncoming()
    {
    }

    @Override
    public void shutdown()
    {
    }

    @Override
    public WorkerQueueMetricsReporter getMetrics()
    {
        return new WorkerQueueMetricsReporter()
        {
            @Override
            public int getQueueErrors()
            {
                return 0;
            }

            @Override
            public int getMessagesReceived()
            {
                return 0;
            }

            @Override
            public int getMessagesPublished()
            {
                return 0;
            }

            @Override
            public int getMessagesRejected()
            {
                return 0;
            }

            @Override
            public int getMessagesDropped()
            {
                return 0;
            }
        };
    }

    @Override
    public void disconnectIncoming()
    {
    }

    @Override
    public void reconnectIncoming()
    {
    }

    @Override
    public void rejectTask(final TaskInformation taskInformation)
    {
    }

    @Override
    public void discardTask(final TaskInformation taskInformation)
    {
    }

    @Override
    public String getInputQueue()
    {
        return "worker-in";
    }

    @Override
    public String getPausedQueue()
    {
        return "";
    }
}
