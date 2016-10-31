package com.hpe.caf.worker.testing;

/**
 * Created by ploch on 08/11/2015.
 */
public class WorkerDescriptor<TConfiguration, TTask, TResult> {

    private final Class<TConfiguration> configurationClass;
    private final Class<TTask> taskClass;
    private final Class<TResult> resultClass;
    private final String workerName;
    private final int apiVersion;

    public WorkerDescriptor(Class<TConfiguration> configurationClass, Class<TTask> taskClass, Class<TResult> resultClass, String workerName, int apiVersion) {

        this.configurationClass = configurationClass;
        this.taskClass = taskClass;
        this.resultClass = resultClass;
        this.workerName = workerName;
        this.apiVersion = apiVersion;
    }

    /**
     * Getter for property 'configurationClass'.
     *
     * @return Value for property 'configurationClass'.
     */
    public Class<TConfiguration> getConfigurationClass() {
        return configurationClass;
    }

    /**
     * Getter for property 'taskClass'.
     *
     * @return Value for property 'taskClass'.
     */
    public Class<TTask> getTaskClass() {
        return taskClass;
    }

    /**
     * Getter for property 'resultClass'.
     *
     * @return Value for property 'resultClass'.
     */
    public Class<TResult> getResultClass() {
        return resultClass;
    }

    /**
     * Getter for property 'workerName'.
     *
     * @return Value for property 'workerName'.
     */
    public String getWorkerName() {
        return workerName;
    }

    /**
     * Getter for property 'apiVersion'.
     *
     * @return Value for property 'apiVersion'.
     */
    public int getApiVersion() {
        return apiVersion;
    }
}
