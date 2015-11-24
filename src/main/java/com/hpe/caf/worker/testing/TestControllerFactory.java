package com.hpe.caf.worker.testing;

import com.hpe.caf.api.CipherException;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.util.ModuleLoaderException;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Created by ploch on 08/11/2015.
 */
public class TestControllerFactory {

    public static TestController createDefault(
            String outputQueue,
            TestItemProvider itemProvider,
            WorkerTaskFactory workerTaskFactory,
            ResultProcessor resultProcessor) throws Exception
    {
        WorkerServices workerServices = WorkerServices.getDefault();

        return create(workerServices, outputQueue, itemProvider, workerTaskFactory, resultProcessor);
    }

    public static <TConfig> TestController createDefault(
            Class<TConfig> configClass, Function<TConfig, String> queueNameFunc,
            TestItemProvider itemProvider,
            WorkerTaskFactory workerTaskFactory,
            ResultProcessor resultProcessor) throws Exception
    {
        WorkerServices workerServices = WorkerServices.getDefault();
        ConfigurationSource configurationSource = workerServices.getConfigurationSource();

        TConfig workerConfiguration = configurationSource.getConfiguration(configClass);
        String queueName = queueNameFunc.apply(workerConfiguration);

        return create(workerServices, queueName, itemProvider, workerTaskFactory, resultProcessor);
    }

    private static TestController create(WorkerServices workerServices,
                                         String queueName,
                                         TestItemProvider itemProvider,
                                         WorkerTaskFactory workerTaskFactory,
                                         ResultProcessor resultProcessor) throws Exception
    {
        ConfigurationSource configurationSource = workerServices.getConfigurationSource();
        RabbitWorkerQueueConfiguration rabbitConfiguration = configurationSource.getConfiguration(RabbitWorkerQueueConfiguration.class);

        rabbitConfiguration.getRabbitConfiguration().setRabbitHost(SettingsProvider.defaultProvider.getSetting(SettingNames.dockerHostAddress));
        rabbitConfiguration.getRabbitConfiguration().setRabbitPort(Integer.parseInt(SettingsProvider.defaultProvider.getSetting(SettingNames.rabbitmqNodePort)));

        QueueServices queueServices = QueueServicesFactory.create(rabbitConfiguration, queueName, workerServices.getCodec());

        QueueManager queueManager = new QueueManager(queueServices, workerServices);

        TestController controller = new TestController(workerServices, itemProvider, queueManager, workerTaskFactory, resultProcessor);
        return controller;
    }
}
