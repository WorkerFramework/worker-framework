package com.hpe.caf.worker.testing;


import com.hpe.caf.api.worker.TrackingInfo;

/**
 * Created by ploch on 08/11/2015.
 */
public interface WorkerTaskFactory<TTask, TInput, TExpected> {

    String getWorkerName();
    int getApiVersion();

    TTask createTask(TestItem<TInput,TExpected> testItem) throws Exception;

    default TrackingInfo createTrackingInfo(TestItem<TInput,TExpected> testItem) {
        return null;
    }
}
