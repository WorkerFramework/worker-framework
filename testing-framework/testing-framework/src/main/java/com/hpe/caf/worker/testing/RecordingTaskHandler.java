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

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.worker.testing.api.TaskMessageHandler;
import com.hpe.caf.worker.testing.api.TestContext;
import com.hpe.caf.worker.testing.api.TestItem;
import com.hpe.caf.worker.testing.api.WorkerInfo;
import com.hpe.caf.worker.testing.storage.TestItemRepository;

import java.io.IOException;

/**
 * Created by ploch on 07/03/2017.
 */
public class RecordingTaskHandler implements TaskMessageHandler
{

    private final Codec codec;
    private final TestItemRepository repository;
    private final WorkerInfo workerInfo;
    private final TestContext testContext;

    public RecordingTaskHandler(TestContext testContext, Codec codec, TestItemRepository repository, WorkerInfo workerInfo)
    {
        this.testContext = testContext;
        this.codec = codec;
        this.repository = repository;
        this.workerInfo = workerInfo;
    }

    @Override
    public void handle(TaskMessage resultMessage)
    {
        byte[] taskData = resultMessage.getTaskData();
        Object workerResult = null;
        try {
            workerResult = codec.deserialise(taskData, workerInfo.getWorkerResultClass());
        }
        catch (CodecException e) {
            throw new TestExecutionException("Failed to deserialize message.", e);
        }
        TestItem currentTestItem = testContext.getCurrentTestItem();
        currentTestItem.setExpectedOutputData(workerResult);
        try {
            repository.saveExpectation(currentTestItem);
        }
        catch (IOException e) {
            throw new TestExecutionException("Failed to save expectation.", e);
        }
        testContext.notifyCompleted();

    }
}
