/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.worker.example;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.worker.AbstractWorkerFactory;

/**
 * Factory class for creating an Example Worker.
 */
public class ExampleWorkerFactory extends AbstractWorkerFactory<ExampleWorkerConfiguration, ExampleWorkerTask>
{
    public ExampleWorkerFactory(ConfigurationSource configSource, DataStore store, Codec codec) throws WorkerException
    {
        super(configSource, store, codec, ExampleWorkerConfiguration.class, ExampleWorkerTask.class);
    }

    @Override
    protected String getWorkerName()
    {
        return ExampleWorkerConstants.WORKER_NAME;
    }

    @Override
    protected int getWorkerApiVersion()
    {
        return ExampleWorkerConstants.WORKER_API_VER;
    }

    /**
     * Create a worker given a task, using DataStore, ConfiguratonSource and Codec passed in the constructor.
     *
     * @param task
     * @return ExampleWorker
     * @throws InvalidTaskException
     */
    @Override
    public Worker createWorker(ExampleWorkerTask task) throws InvalidTaskException
    {
        return new ExampleWorker(task, getDataStore(), getConfiguration().getOutputQueue(), getCodec(), getConfiguration().getResultSizeThreshold());
    }

    @Override
    public String getInvalidTaskQueue()
    {
        return getConfiguration().getOutputQueue();
    }

    @Override
    public int getWorkerThreads()
    {
        return getConfiguration().getThreads();
    }

    /**
     * ExampleWorkerFactory is responsible for calling the health-check to view the status of the worker and this is displayed on
     * Marathon.
     *
     * @return HealthResult
     */
    @Override
    public HealthResult healthCheck()
    {
        ExampleWorkerHealthCheck healthCheck = new ExampleWorkerHealthCheck();
        return healthCheck.healthCheck();
    }
}
