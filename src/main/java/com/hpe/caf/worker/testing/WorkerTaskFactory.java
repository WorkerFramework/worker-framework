package com.hpe.caf.worker.testing;


/**
 * Created by ploch on 08/11/2015.
 */
public interface WorkerTaskFactory<TTask, TInput, TExpected> {

    String getWorkerName();
    int getApiVersion();

    TTask createTask(TestItem<TInput,TExpected> testItem) throws Exception;

}
