package com.hpe.caf.worker.testing;

import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;

import java.util.function.Function;

/**
 * Created by ploch on 08/11/2015.
 */
public class TestControllerFactory extends TestControllerFactoryBase<TestController> {

    @Override
    public TestController createController(WorkerServices workerServices, TestItemProvider itemProvider, QueueManager queueManager, WorkerTaskFactory workerTaskFactory, ResultProcessor resultProcessor, boolean stopOnError) throws Exception {
        return new TestController(workerServices, itemProvider, queueManager, workerTaskFactory, resultProcessor, stopOnError);
    }
}
