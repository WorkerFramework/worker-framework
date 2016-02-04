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
