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
package com.hpe.caf.worker.testing.experiment;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.binaryhash.BinaryHashWorkerConstants;
import com.hpe.caf.worker.binaryhash.BinaryHashWorkerResult;
import com.hpe.caf.worker.binaryhash.BinaryHashWorkerTask;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;
import com.hpe.caf.worker.testing.*;
import com.hpe.caf.worker.testing.api.*;
import com.hpe.caf.worker.testing.preparation.ContentFilesGenerator;
import com.hpe.caf.worker.testing.preparation.DefaultPathTestCaseInfoFactory;
import com.hpe.caf.worker.testing.storage.FileTestItemRepository;
import com.hpe.caf.worker.testing.storage.TestItemDescriptor;
import com.hpe.caf.worker.testing.storage.TestItemRepository;
import com.hpe.caf.worker.testing.storage.YamlTestCaseSerializer;
import com.hpe.caf.worker.testing.util.*;
import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.Collection;

/**
 * Created by ploch on 06/03/2017.
 */
public class ExampleWorkerIT {

    public class HashWorkerTaskFactory implements WorkerTaskFactory<BinaryHashWorkerTestInput, BinaryHashWorkerResult> {

        private DataStore dataStore;

        public HashWorkerTaskFactory(DataStore dataStore) {
            this.dataStore = dataStore;
        }

        @Override
        public Object createTask(TestItem<BinaryHashWorkerTestInput, BinaryHashWorkerResult> testItem) throws DataStoreException {
            BinaryHashWorkerTask workerTask = new BinaryHashWorkerTask();
            ObjectMapper mapper = new ObjectMapper();
            BinaryHashWorkerTestInput binaryHashWorkerTestInput = mapper.convertValue(testItem.getInputData(), BinaryHashWorkerTestInput.class);
            InputFileData fileData = binaryHashWorkerTestInput.getInputFileData();

            String path = Paths.get(testItem.getLocation()).getParent().toAbsolutePath().toString();
            if (!Strings.isNullOrEmpty(fileData.getStorageReference())) {
                workerTask.sourceData = ReferencedData.getReferencedData(fileData.getStorageReference());
            } else {
                String reference = dataStore.store(Paths.get(path, fileData.getFilePath()), null);
                workerTask.sourceData = ReferencedData.getReferencedData(reference);
            }
            return workerTask;
        }
    }

    @Test
    public void testGenerateDescriptors() throws Exception {
     //   BinaryHashWorkerDescriptorGenerator generator = new BinaryHashWorkerDescriptorGenerator()

        String root = Paths.get(TestFilesUtil.getTestDataRootPath(), "content-files", "in-folders").toAbsolutePath().toString();
        ContentFilesGenerator generator = new ContentFilesGenerator(root, true, null,
                new DefaultPathTestCaseInfoFactory(root),
                new BinaryHashWorkerTestInputFactory(),
                new FileTestItemRepository(root, new YamlTestCaseSerializer()));

        Collection<TestItemDescriptor> generate = generator.generate();
    }


    @Test
    public void testWorker3() throws Exception {
        WorkerInfo workerInfo = new WorkerInfo(BinaryHashWorkerConstants.WORKER_API_VER, BinaryHashWorkerConstants.WORKER_NAME, BinaryHashWorkerTask.class, BinaryHashWorkerResult.class);

        TestBuilder builder = new TestBuilder();
        HashWorkerTaskFactory factory = new HashWorkerTaskFactory(builder.getWorkerServices().getDataStore());


        TestController controller = builder.createDefault(workerInfo, factory);
        String root = Paths.get(TestFilesUtil.getTestDataRootPath(), "content-files", "in-folders").toAbsolutePath().toString();
        FileTestItemRepository repository = new FileTestItemRepository(root, new YamlTestCaseSerializer());
        Collection<TestItem> testItems = repository.retrieveTestItems();

        for (TestItem testItem : testItems) {
            TestResult testResult = controller.executeTest(testItem);
        }

        TestItem testItem = createTestItem();

        TestResult testResult = controller.executeTest(testItem);

        System.out.println(testResult.isSuccess());

    }

   /* @Test
    public void testWorker2() throws Exception {

        WorkerInfo workerInfo = new WorkerInfo(BinaryHashWorkerConstants.WORKER_API_VER, BinaryHashWorkerConstants.WORKER_NAME, BinaryHashWorkerTask.class, BinaryHashWorkerResult.class);


        WorkerServices workerServices = WorkerServicesFactory.create();
        RabbitWorkerQueueConfiguration rabbitConfig = workerServices.getConfigurationSource().getConfiguration(RabbitWorkerQueueConfiguration.class);
        String queueName = rabbitConfig.getInputQueue();
        WorkerQueueProvider provider = ModuleLoader.getService(WorkerQueueProvider.class);
        ManagedWorkerQueue workerQueue = provider.getWorkerQueue(workerServices.getConfigurationSource(), 1);

        QueueManager queueManager = new QueueManager(workerServices.getCodec(), workerQueue, queueName);
        TaskMessageFactory taskMessageFactory = new TaskMessageFactory(workerServices.getCodec(), workerInfo.getWorkerName() , queueName, workerInfo.getApiVersion() );
        WorkerTaskFactory workerTaskFactory = new WorkerTaskFactory<BinaryHashWorkerTestInput, BinaryHashWorkerResult>() {
            @Override
            public Object createTask(TestItem<BinaryHashWorkerTestInput, BinaryHashWorkerResult> testItem) throws DataStoreException {
                BinaryHashWorkerTask workerTask = new BinaryHashWorkerTask();
                InputFileData fileData = testItem.getInputData().getInputFileData();

                if (!Strings.isNullOrEmpty(fileData.getStorageReference())) {
                    workerTask.sourceData = ReferencedData.getReferencedData(fileData.getStorageReference());
                } else {
                    String reference = workerServices.getDataStore().store(Paths.get(fileData.getFilePath()), null);
                    workerTask.sourceData = ReferencedData.getReferencedData(reference);
                }
                return workerTask;
            }
        };

        TestItemRepository repository = new FileTestItemRepository(SettingsProvider.defaultProvider.getSetting("input.folder"), new YamlTestCaseSerializer());

//        new RecordingTaskHandler(context, codec, repository, workerInfo)
        TaskMessageHandlerFactory messageHandlerFactory = new RecordingTaskMessageHandlerFactory(repository, workerServices.getCodec(), workerInfo);
        TestController controller = new TestController(queueManager, taskMessageFactory, workerTaskFactory, messageHandlerFactory);

        BinaryHashWorkerTestInput testInput = new BinaryHashWorkerTestInput();
        InputFileData inputFileData = new InputFileData();
        inputFileData.setFilePath("C:\\git\\caf\\worker-binaryhash\\worker-binaryhash-container-fs\\test-data\\input\\FirstTest.txt");
        testInput.setInputFileData(inputFileData);

        TestItem<BinaryHashWorkerTestInput, BinaryHashWorkerResult> testItem = new TestItem<BinaryHashWorkerTestInput, BinaryHashWorkerResult>(
                new TestCaseInfo("binary-hash-test-1", null, "Binary hash worker test", null), testInput, null);

        TestResult testResult = controller.executeTest(testItem);

        repository.saveDescriptor(testItem);
    }*/

    private TestItem createTestItem() {
        BinaryHashWorkerTestInput testInput = new BinaryHashWorkerTestInput();
        InputFileData inputFileData = new InputFileData();
        inputFileData.setFilePath("C:\\git\\caf\\worker-binaryhash\\worker-binaryhash-container-fs\\test-data\\input\\FirstTest.txt");
        testInput.setInputFileData(inputFileData);

        TestItem<BinaryHashWorkerTestInput, BinaryHashWorkerResult> testItem = new TestItem<BinaryHashWorkerTestInput, BinaryHashWorkerResult>(
                new TestCaseInfo("binary-hash-test-1", null, "Binary hash worker test", null), testInput, null, "C:\\git\\caf\\worker-framework\\testing-framework\\testing-framework\\src\\test\\test-data\\content-files\\in-folders");

        return testItem;
    }


}
