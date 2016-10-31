package com.hpe.caf.worker.testing;

import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;

import java.util.function.Function;

/**
 * Created by oloughli on 31/05/2016.
 */
public class TestControllerFactorySingle extends TestControllerFactoryBase<TestControllerSingle>{

    @Override
    public TestControllerSingle createController(WorkerServices workerServices, TestItemProvider itemProvider, QueueManager queueManager, WorkerTaskFactory workerTaskFactory, ResultProcessor resultProcessor, boolean stopOnError) throws Exception {
        return new TestControllerSingle (workerServices, queueManager, workerTaskFactory, resultProcessor, stopOnError);
    }
}
