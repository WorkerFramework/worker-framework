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
package com.hpe.caf.worker.example;

import com.hpe.caf.worker.testing.*;
import com.hpe.caf.worker.testing.execution.AbstractTestControllerProvider;

/**
 * Class providing task factory, validation processor, save result processor, result preparation provider for running integration
 * tests.
 */
public class ExampleTestControllerProvider extends AbstractTestControllerProvider<ExampleWorkerConfiguration,
        ExampleWorkerTask, ExampleWorkerResult, ExampleTestInput, ExampleTestExpectation> {

    public ExampleTestControllerProvider() {
        super(ExampleWorkerConstants.WORKER_NAME, ExampleWorkerConfiguration::getOutputQueue, ExampleWorkerConfiguration.class, ExampleWorkerTask.class, ExampleWorkerResult.class, ExampleTestInput.class, ExampleTestExpectation.class);
    }

    /**
     * Return a task factory for creating tasks.
     * @param configuration
     * @return ExampleWorkerTaskFactory
     * @throws Exception
     */
    @Override
    protected WorkerTaskFactory<ExampleWorkerTask, ExampleTestInput, ExampleTestExpectation> getTaskFactory(TestConfiguration<ExampleWorkerTask, ExampleWorkerResult, ExampleTestInput, ExampleTestExpectation> configuration) throws Exception {
        return new ExampleWorkerTaskFactory(configuration);
    }

    /**
     * Return a result validation processor for validating the worker result is the same as the expected result in the test item.
     * @param configuration
     * @param workerServices
     * @return ExampleWorkerResultValidationProcessor
     */
    @Override
    protected ResultProcessor getTestResultProcessor(TestConfiguration<ExampleWorkerTask, ExampleWorkerResult, ExampleTestInput, ExampleTestExpectation> configuration, WorkerServices workerServices) {
        return new ExampleWorkerResultValidationProcessor(workerServices);
    }

    /**
     * Return a result preparation provider for preparing test items from YAML files.
     * @param configuration
     * @return ExampleResultPreparationProvider
     */
    @Override
    protected TestItemProvider getDataPreparationItemProvider(TestConfiguration<ExampleWorkerTask, ExampleWorkerResult, ExampleTestInput, ExampleTestExpectation> configuration) {
        return new ExampleResultPreparationProvider(configuration);
    }

    /**
     * Return a save result processor for generating .testcase and result.content files found in test-data > input folder.
     * @param configuration
     * @param workerServices
     * @return ExampleWorkerSaveResultProcessor
     */
    @Override
    protected ResultProcessor getDataPreparationResultProcessor(TestConfiguration<ExampleWorkerTask, ExampleWorkerResult, ExampleTestInput, ExampleTestExpectation> configuration, WorkerServices workerServices) {
        return new ExampleWorkerSaveResultProcessor(configuration, workerServices);
    }

}
