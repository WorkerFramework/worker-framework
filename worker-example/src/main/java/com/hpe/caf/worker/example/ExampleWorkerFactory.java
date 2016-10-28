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
public class ExampleWorkerFactory extends AbstractWorkerFactory<ExampleWorkerConfiguration, ExampleWorkerTask> {

    public ExampleWorkerFactory(ConfigurationSource configSource, DataStore store, Codec codec) throws WorkerException {
        super(configSource, store, codec, ExampleWorkerConfiguration.class, ExampleWorkerTask.class);
    }

    @Override
    protected String getWorkerName() {
        return ExampleWorkerConstants.WORKER_NAME;
    }

    @Override
    protected int getWorkerApiVersion() {
        return ExampleWorkerConstants.WORKER_API_VER;
    }

    /**
     * Create a worker given a task, using DataStore, ConfiguratonSource and Codec passed in the constructor.
     * @param task
     * @return ExampleWorker
     * @throws InvalidTaskException
     */
    @Override
    public Worker createWorker(ExampleWorkerTask task) throws InvalidTaskException {
        return new ExampleWorker(task, getDataStore(), getConfiguration().getOutputQueue(), getCodec(), getConfiguration().getResultSizeThreshold());
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
     * ExampleWorkerFactory is responsible for calling the health-check to view the status of the worker and this is displayed on Marathon.
     * @return HealthResult
     */
    @Override
    public HealthResult healthCheck() {
        ExampleWorkerHealthCheck healthCheck = new ExampleWorkerHealthCheck();
        return healthCheck.healthCheck();
    }
}
