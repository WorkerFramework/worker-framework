package com.hpe.caf.worker.testing.configuration;

/**
 * Created by ploch on 04/12/2015.
 */
public class WorkerClasses<TWorkerTask, TWorkerResult> {

    private Class<TWorkerTask> workerTaskClass;

    private Class<TWorkerResult> workerResultClass;

    public WorkerClasses(Class<TWorkerTask> workerTaskClass, Class<TWorkerResult> workerResultClass) {
        this.workerTaskClass = workerTaskClass;
        this.workerResultClass = workerResultClass;
    }

    /**
     * Getter for property 'workerTaskClass'.
     *
     * @return Value for property 'workerTaskClass'.
     */
    public Class<TWorkerTask> getWorkerTaskClass() {
        return workerTaskClass;
    }

    /**
     * Getter for property 'workerResultClass'.
     *
     * @return Value for property 'workerResultClass'.
     */
    public Class<TWorkerResult> getWorkerResultClass() {
        return workerResultClass;
    }
}
