/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.worker.testing;

import com.hpe.caf.api.CipherException;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.WorkerQueueProvider;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;
import com.hpe.caf.worker.testing.api.*;
import com.hpe.caf.worker.testing.storage.FileTestItemRepository;
import com.hpe.caf.worker.testing.storage.TestItemRepository;
import com.hpe.caf.worker.testing.storage.YamlTestCaseSerializer;
import com.hpe.caf.worker.testing.util.*;

/**
 * Created by ploch on 17/03/2017.
 */
public class TestBuilder {

    private final WorkerServices workerServices;

    public TestBuilder() {
        try {
            this.workerServices = WorkerServicesFactory.create();
        } catch (ModuleLoaderException | CipherException | ConfigurationException | DataStoreException  e) {
            throw new TestExecutionException("Couldn't initialize WorkerServices.", e);
        }
    }

    public TestBuilder(WorkerServices workerServices) {
        this.workerServices = workerServices;
    }

    public WorkerServices getWorkerServices() {
        return workerServices;
    }

    public TestController createDefault(WorkerInfo workerInfo, WorkerTaskFactory workerTaskFactory)  {
        try {

            RabbitWorkerQueueConfiguration rabbitConfig = workerServices.getConfigurationSource().getConfiguration(RabbitWorkerQueueConfiguration.class);
            String queueName = rabbitConfig.getInputQueue();
            rabbitConfig.setInputQueue("BinaryHashWorker-output-1");

            CodeConfigurationSource codeConfigurationSource = new CodeConfigurationSource(rabbitConfig);
            workerServices.getConfigurationSource().insertConfigurationSource(0,codeConfigurationSource);

            WorkerQueueProvider provider = ModuleLoader.getService(WorkerQueueProvider.class);
            ManagedWorkerQueue workerQueue = provider.getWorkerQueue(workerServices.getConfigurationSource(), 1);

            QueueManager queueManager = new QueueManager(workerServices.getCodec(), workerQueue, queueName);
            TaskMessageFactory taskMessageFactory = new TaskMessageFactory(workerServices.getCodec(), workerInfo.getWorkerName(), queueName, workerInfo.getApiVersion());


            TestItemRepository repository = new FileTestItemRepository(SettingsProvider.defaultProvider.getSetting("input.folder"), new YamlTestCaseSerializer());
            TaskMessageHandlerFactory messageHandlerFactory = new RecordingTaskMessageHandlerFactory(repository, workerServices.getCodec(), workerInfo);
            TestController controller = new TestController(queueManager, taskMessageFactory, workerTaskFactory, messageHandlerFactory);

            return controller;

        } catch (Exception e) {
            throw new TestExecutionException("Test initialization failed.", e);
        }

    }
}
