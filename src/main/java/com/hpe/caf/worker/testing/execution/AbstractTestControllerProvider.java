package com.hpe.caf.worker.testing.execution;

import com.hpe.caf.worker.testing.*;

import java.util.function.Function;

/**
 * Created by ploch on 22/12/2015.
 */
public abstract class AbstractTestControllerProvider<TWorkerConfiguration, TWorkerTask, TWorkerResult, TInput, TExpectation> implements TestControllerProvider {

    private final Function<TWorkerConfiguration, String> queueNameFunc;
    private final Class<TWorkerConfiguration> workerConfigurationClass;
    private final Class<TWorkerTask> workerTaskClass;
    private final Class<TWorkerResult> workerResultClass;
    private final Class<TInput> inputClass;
    private final Class<TExpectation> expectationClass;

    public AbstractTestControllerProvider(Function<TWorkerConfiguration, String> queueNameFunc, Class<TWorkerConfiguration> workerConfigurationClass, Class<TWorkerTask> workerTaskClass, Class<TWorkerResult> workerResultClass, Class<TInput> inputClass, Class<TExpectation> expectationClass) {
        this.queueNameFunc = queueNameFunc;
        this.workerConfigurationClass = workerConfigurationClass;
        this.workerTaskClass = workerTaskClass;
        this.workerResultClass = workerResultClass;
        this.inputClass = inputClass;
        this.expectationClass = expectationClass;
    }

    protected abstract WorkerTaskFactory<TWorkerTask, TInput, TExpectation> getTaskFactory(TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration) throws Exception;

    protected abstract TestItemProvider getTestItemProvider(TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration);

    protected abstract ResultProcessor getTestResultProcessor(TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration, WorkerServices workerServices);

    protected abstract TestItemProvider getDataPreparationItemProvider(TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration);

    protected abstract ResultProcessor getDataPreparationResultProcessor(TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration, WorkerServices workerServices);

    @Override
    public TestController getTestController() throws Exception {
        TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration = TestConfiguration.createDefault(workerTaskClass, workerResultClass, inputClass, expectationClass);
        return TestControllerFactory.createDefault(workerConfigurationClass, queueNameFunc, getTestItemProvider(configuration), getTaskFactory(configuration), getTestResultProcessor(configuration, WorkerServices.getDefault()));
    }

    @Override
    public TestController getDataPreparationController() throws Exception {
        TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration = TestConfiguration.createDefault(workerTaskClass, workerResultClass, inputClass, expectationClass);
        return TestControllerFactory.createDefault(workerConfigurationClass, queueNameFunc, getDataPreparationItemProvider(configuration), getTaskFactory(configuration), getDataPreparationResultProcessor(configuration, WorkerServices.getDefault()));

    }


}
