package com.hpe.caf.worker.testing.execution;

import com.hpe.caf.worker.testing.*;

import java.util.function.Function;

/**
 * Created by ploch on 22/12/2015.
 */
public abstract class AbstractTestControllerProvider<TWorkerConfiguration, TWorkerTask, TWorkerResult, TInput, TExpectation> implements TestControllerProvider {

    private final String workerName;
    private final Function<TWorkerConfiguration, String> queueNameFunc;
    private final Class<TWorkerConfiguration> workerConfigurationClass;
    private final Class<TWorkerTask> workerTaskClass;
    private final Class<TWorkerResult> workerResultClass;
    private final Class<TInput> inputClass;
    private final Class<TExpectation> expectationClass;
    TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration;

    public AbstractTestControllerProvider(String workerName, Function<TWorkerConfiguration, String> queueNameFunc, Class<TWorkerConfiguration> workerConfigurationClass, Class<TWorkerTask> workerTaskClass, Class<TWorkerResult> workerResultClass, Class<TInput> inputClass, Class<TExpectation> expectationClass) {
        this.workerName = workerName;
        this.queueNameFunc = queueNameFunc;
        this.workerConfigurationClass = workerConfigurationClass;
        this.workerTaskClass = workerTaskClass;
        this.workerResultClass = workerResultClass;
        this.inputClass = inputClass;
        this.expectationClass = expectationClass;
    }

    protected abstract WorkerTaskFactory<TWorkerTask, TInput, TExpectation> getTaskFactory(TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration) throws Exception;

    protected TestItemProvider getTestItemProvider(TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration){
        return new SerializedFilesTestItemProvider<>(configuration);
    }

    protected abstract ResultProcessor getTestResultProcessor(TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration, WorkerServices workerServices);

    protected abstract TestItemProvider getDataPreparationItemProvider(TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration);

    protected abstract ResultProcessor getDataPreparationResultProcessor(TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration, WorkerServices workerServices);

    @Override
    public String getWorkerName() {
        return workerName;
    }

    @Override
    public TestController getTestController() throws Exception {
        TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration = TestConfiguration.createDefault(workerTaskClass, workerResultClass, inputClass, expectationClass);
        TestControllerFactoryBase<TestController> factory = new TestControllerFactory();
        return factory.createDefault(workerConfigurationClass, queueNameFunc, getTestItemProvider(configuration), getTaskFactory(configuration), getTestResultProcessor(configuration, WorkerServices.getDefault()));
    }

    @Override
    public TestController getDataPreparationController() throws Exception {
        TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration = TestConfiguration.createDefault(workerTaskClass, workerResultClass, inputClass, expectationClass);
        TestControllerFactoryBase<TestController> factory = new TestControllerFactory();
        return factory.createDefault(workerConfigurationClass, queueNameFunc, getDataPreparationItemProvider(configuration), getTaskFactory(configuration), getDataPreparationResultProcessor(configuration, WorkerServices.getDefault()));
    }

    // Aaron's Test Additions Test
    @Override
    public TestControllerSingle getNewTestController() throws Exception {
        setConfiguration();
        TestControllerFactoryBase<TestControllerSingle> factory = new TestControllerFactorySingle();
        return factory.createDefault(workerConfigurationClass, queueNameFunc, null,getTaskFactory(configuration), getTestResultProcessor(configuration, WorkerServices.getDefault()));
    }

    @Override
    public TestControllerSingle getNewDataPreparationController() throws Exception {
        setConfiguration();
        TestControllerFactoryBase<TestControllerSingle> factory = new TestControllerFactorySingle();
        return factory.createDefault(workerConfigurationClass, queueNameFunc, null,getTaskFactory(configuration), getDataPreparationResultProcessor(configuration, WorkerServices.getDefault()));
    }

    @Override
    public TestItemProvider getItemProvider(boolean typeOfItemProvider)
    {
        setConfiguration();
        TestItemProvider itemProvider = typeOfItemProvider? getDataPreparationItemProvider(configuration) : getTestItemProvider(configuration);
        return itemProvider;
    }

    public void setConfiguration() {
        configuration = TestConfiguration.createDefault(workerTaskClass, workerResultClass, inputClass, expectationClass);
    }
}
