package com.hpe.caf.api.worker;

/**
 * This interface should be implemented by CAF Workers which are able to process
 * multiple tasks together.
 * 
 * It is expected to be implemented by the WorkerFactory object which has been
 * supplied by the getWorkerFactory() method of the WorkerFactoryProvider class.
 */
public interface BulkWorker {

    /**
     * The Worker should begin processing the tasks.  It can use the runtime
     * object to retrieve the tasks.
     * @param runtime is used to retrieve the tasks
     * @throws InterruptedException if the thread is interrupted by another
     * thread
     */
    void processTasks(BulkWorkerRuntime runtime)
        throws InterruptedException;
}
