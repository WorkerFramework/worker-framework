package com.hpe.caf.worker.testing;


/**
 * Created by ploch on 08/11/2015.
 */
public interface WorkerTaskFactory<TTask> {

    String getWorkerName();
    int getApiVersion();

    TTask createTask(TestItem testItem);

}
