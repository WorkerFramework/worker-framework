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

import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.TaskInformation;
import com.hpe.caf.api.worker.WorkerQueueMetricsReporter;
import com.hpe.caf.api.worker.WorkerQueueProvider;

import java.util.Map;

public class TestWorkerQueueProvider implements WorkerQueueProvider
{
    @Override
    public ManagedWorkerQueue getWorkerQueue(ConfigurationSource configurationSource, int maxTasks) throws QueueException
    {
        return new ManagedWorkerQueue()
        {
            @Override
            public void start(TaskCallback callback) throws QueueException
            {

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
                return null;
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
            public HealthResult healthCheck()
            {
                return null;
            }

            @Override
            public void publish(TaskInformation taskInformation, byte[] taskMessage, String targetQueue, Map<String, Object> headers, boolean isLastMessage) throws QueueException
            {

            }

            @Override
            public void publish(TaskInformation taskInformation, byte[] taskMessage, String targetQueue, Map<String, Object> headers) throws QueueException
            {

            }

            @Override
            public void rejectTask(TaskInformation taskInformation)
            {

            }

            @Override
            public void discardTask(TaskInformation taskInformation)
            {

            }

            @Override
            public void acknowledgeTask(TaskInformation taskInformation)
            {

            }

            @Override
            public String getInputQueue()
            {
                return "";
            }

            @Override
            public String getPausedQueue()
            {
                return "";
            }
        };
    }
}
