/*
 * Copyright 2018-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.worker.example;

import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.WorkerServices;
import com.hpe.caf.worker.testing.preparation.ContentResultProcessor;

/**
 * Processor for generating testcase and result.content files
 */
public class ExampleWorkerSaveResultProcessor extends ContentResultProcessor<ExampleWorkerTask, ExampleWorkerResult, ExampleTestInput, ExampleTestExpectation>
{
    protected ExampleWorkerSaveResultProcessor(TestConfiguration configuration, WorkerServices workerServices)
    {
        super(configuration, workerServices.getCodec(), workerServices.getDataStore(), ExampleWorkerResultAccessors::getTextData);
    }

    /**
     * Called by the test app with -g command argument to generate test files.
     *
     * @param exampleWorkerResult
     * @param message
     * @param testItem
     * @return byte[]
     * @throws Exception
     */
    @Override
    protected byte[] getOutputContent(ExampleWorkerResult exampleWorkerResult, TaskMessage message, TestItem<ExampleTestInput, ExampleTestExpectation> testItem)
        throws Exception
    {
        testItem.getExpectedOutputData().setResult(exampleWorkerResult);
        return super.getOutputContent(exampleWorkerResult, message, testItem);
    }
}
