package com.hpe.caf.worker.testing;


import java.util.Vector;

/**
 * Created by ploch on 08/11/2015.
 */
public interface WorkerTaskFactory<TTask, TInput, TExpected> {

    String getWorkerName();
    int getApiVersion();

    TTask createTask(TestItem<TInput,TExpected> testItem);

}
