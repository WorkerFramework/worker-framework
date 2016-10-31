package com.hpe.caf.worker.testing.configuration;

import com.hpe.caf.worker.testing.QueueServices;
import com.hpe.caf.worker.testing.WorkerServices;

/**
 * Created by ploch on 05/12/2015.
 */
public class TestingContext<TWorkerTask, TWorkerResult, TInput, TExpectation> {

    private TestCaseSettings<TWorkerTask, TWorkerResult, TInput, TExpectation> testCaseSettings;
    private WorkerServices workerServices;
    private QueueServices queueServices;



}
