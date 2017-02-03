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
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.hpe.caf.worker.testing.*;
import com.hpe.caf.worker.testing.execution.AbstractTestControllerProvider;

/**
 * Class providing task factory, validation processor, save result processor, result preparation provider for running integration
 * tests.
 */
public class ${workerName}TestControllerProvider extends AbstractTestControllerProvider<${workerName}Configuration,
        ${workerName}Task, ${workerName}Result, ${workerName}TestInput, ${workerName}TestExpectation> {

    public ${workerName}TestControllerProvider() {
        super(${workerName}Constants.WORKER_NAME, ${workerName}Configuration::getOutputQueue, ${workerName}Configuration.class, ${workerName}Task.class, ${workerName}Result.class, ${workerName}TestInput.class, ${workerName}TestExpectation.class);
    }

    /**
     * Return a task factory for creating tasks.
     * @param configuration
     * @return ${workerName}TaskFactory
     * @throws Exception
     */
    @Override
    protected WorkerTaskFactory<${workerName}Task, ${workerName}TestInput, ${workerName}TestExpectation> getTaskFactory(TestConfiguration<${workerName}Task, ${workerName}Result, ${workerName}TestInput, ${workerName}TestExpectation> configuration) throws Exception {
        return new ${workerName}TaskFactory(configuration);
    }

    /**
     * Return a result validation processor for validating the worker result is the same as the expected result in the test item.
     * @param configuration
     * @param workerServices
     * @return ${workerName}ResultValidationProcessor
     */
    @Override
    protected ResultProcessor getTestResultProcessor(TestConfiguration<${workerName}Task, ${workerName}Result, ${workerName}TestInput, ${workerName}TestExpectation> configuration, WorkerServices workerServices) {
        return new ${workerName}ResultValidationProcessor(workerServices);
    }

    /**
     * Return a result preparation provider for preparing test items from YAML files.
     * @param configuration
     * @return ${workerName}ResultPreparationProvider
     */
    @Override
    protected TestItemProvider getDataPreparationItemProvider(TestConfiguration<${workerName}Task, ${workerName}Result, ${workerName}TestInput, ${workerName}TestExpectation> configuration) {
        return new ${workerName}ResultPreparationProvider(configuration);
    }

    /**
     * Return a save result processor for generating .testcase and result.content files found in test-data > input folder.
     * @param configuration
     * @param workerServices
     * @return ${workerName}SaveResultProcessor
     */
    @Override
    protected ResultProcessor getDataPreparationResultProcessor(TestConfiguration<${workerName}Task, ${workerName}Result, ${workerName}TestInput, ${workerName}TestExpectation> configuration, WorkerServices workerServices) {
        return new ${workerName}SaveResultProcessor(configuration, workerServices);
    }

}
