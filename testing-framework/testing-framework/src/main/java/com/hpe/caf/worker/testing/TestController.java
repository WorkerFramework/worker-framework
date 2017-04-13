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

import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.worker.testing.api.*;
import com.hpe.caf.worker.testing.util.QueueManager;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Manages a test execution.
 */
public class TestController
{

    private final QueueManager queueManager;
    private final TaskMessageFactory factory;
    private final WorkerTaskFactory workerTaskFactory;
    private final TaskMessageHandlerFactory taskMessageHandlerFactory;

    public TestController(QueueManager queueManager, TaskMessageFactory factory, WorkerTaskFactory workerTaskFactory, TaskMessageHandlerFactory taskMessageHandlerFactory)
    {

        this.queueManager = queueManager;
        this.factory = factory;
        this.workerTaskFactory = workerTaskFactory;
        this.taskMessageHandlerFactory = taskMessageHandlerFactory;
    }

    public TestResult executeTest(TestItem testItem) throws QueueException, CodecException, InterruptedException, DataStoreException, IOException
    {

        CompletionSignal signal = new CompletionSignal();
        TestContext context = new TestContext(testItem, signal);
        // TestItemRepository repository = new FileTestItemRepository(SettingsProvider.defaultProvider.getSetting("input.folder"), new YamlTestCaseSerializer());

        String taskId = UUID.randomUUID().toString();
        //    queueManager.start(new RecordingTaskHandler(context, codec, repository, workerInfo));
        TaskMessageHandler taskMessageHandler = taskMessageHandlerFactory.create(context);
        queueManager.start(taskMessageHandler);
        Object workerTask = workerTaskFactory.createTask(testItem);
        context.setWorkerTask(workerTask);
        TaskMessage taskMessage = factory.create(workerTask, null, taskId);

        queueManager.publish(taskMessage);

        signal.doWait();

        List<ValidationResult> validationResults = context.getValidationResults();
        boolean success = validationResults.stream().allMatch(validationResult -> validationResult.getStatus() == ValidationStatus.VALIDATION_SUCCESS);

        TestResult testResult = new TestResult(validationResults, success);

        return testResult;
        //queueManager.getWorkerQueue().publish("1", workerServices.getCodec().serialise(taskMessage), queueName,  new HashMap<>());

    }
}
