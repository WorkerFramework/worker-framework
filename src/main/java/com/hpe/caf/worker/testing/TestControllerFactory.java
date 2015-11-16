package com.hpe.caf.worker.testing;

import com.hpe.caf.api.CipherException;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.util.ModuleLoaderException;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by ploch on 08/11/2015.
 */
public class TestControllerFactory {

    public static TestController createDefault(
            String outputQueue,
            TestItemProvider itemProvider,
            WorkerTaskFactory workerTaskFactory,
            ResultProcessor resultProcessor) throws ModuleLoaderException, DataStoreException, ConfigurationException, CipherException, IOException, TimeoutException {


        WorkerServices workerServices = WorkerServicesFactory.create();
        ConfigurationSource configurationSource = workerServices.getConfigurationSource();
        RabbitWorkerQueueConfiguration configuration = configurationSource.getConfiguration(RabbitWorkerQueueConfiguration.class);

        QueueServices queueServices = QueueServicesFactory.create(configuration, outputQueue);

        QueueManager queueManager = new QueueManager(queueServices, workerServices);

        TestController controller = new TestController(workerServices, itemProvider, queueManager, workerTaskFactory, resultProcessor);
        return controller;
    }
}
