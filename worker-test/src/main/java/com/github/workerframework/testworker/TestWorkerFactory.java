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
package com.github.workerframework.testworker;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerTaskData;
import jakarta.annotation.Nonnull;

final class TestWorkerFactory implements WorkerFactory
{
    private final TestWorkerConfiguration config;

    public TestWorkerFactory(
        final ConfigurationSource configSource,
        final DataStore dataStore,
        final Codec codec
    ) throws WorkerException
    {
        this.config = getConfiguration(configSource);
    }

    @Nonnull
    private static TestWorkerConfiguration getConfiguration(final ConfigurationSource configSource)
        throws WorkerException
    {
        try {
            return configSource.getConfiguration(TestWorkerConfiguration.class);
        } catch (final ConfigurationException ex) {
            throw new WorkerException("Failed to construct TestWorkerConfiguration object", ex);
        }
    }

    @Override
    public String getInvalidTaskQueue()
    {
        return config.getOutputQueue();
    }

    @Nonnull
    @Override
    public Worker getWorker(final WorkerTaskData workerTask) throws TaskRejectedException, InvalidTaskException
    {
        return new TestWorker(config, workerTask);
    }

    @Override
    public int getWorkerThreads()
    {
        return config.getThreads();
    }

    @Nonnull
    @Override
    public HealthResult healthCheck()
    {
        return HealthResult.RESULT_HEALTHY;
    }
}
