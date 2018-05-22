/*
 * Copyright 2018-2017 EntIT Software LLC, a Micro Focus company.
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
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.worker.AbstractWorkerFactory;

/**
 * Factory class for creating a ${workerName}.
 */
public class ${workerName}Factory extends AbstractWorkerFactory<${workerName}Configuration, ${workerName}Task> {

    public ${workerName}Factory(ConfigurationSource configSource, DataStore store, Codec codec) throws WorkerException {
        super(configSource, store, codec, ${workerName}Configuration.class, ${workerName}Task.class);
    }

    @Override
    protected String getWorkerName() {
        return ${workerName}Constants.WORKER_NAME;
    }

    @Override
    protected int getWorkerApiVersion() {
        return ${workerName}Constants.WORKER_API_VER;
    }

    /**
     * Create a worker given a task, using DataStore, ConfiguratonSource and Codec passed in the constructor.
     * @param task
     * @return ${workerName}
     * @throws InvalidTaskException
     */
    @Override
    public Worker createWorker(${workerName}Task task) throws InvalidTaskException {
        return new ${workerName}(task, getDataStore(), getConfiguration().getOutputQueue(), getCodec(), getConfiguration().getResultSizeThreshold());
    }

    @Override
    public String getInvalidTaskQueue() {
        return getConfiguration().getOutputQueue();
    }

    @Override
    public int getWorkerThreads() {
        return getConfiguration().getThreads();
    }

    /**
     * ${workerName}Factory is responsible for calling the health-check to view the status of the worker and this is displayed on Marathon.
     * @return HealthResult
     */
    @Override
    public HealthResult healthCheck() {
        ${workerName}HealthCheck healthCheck = new ${workerName}HealthCheck();
        return healthCheck.healthCheck();
    }
}
