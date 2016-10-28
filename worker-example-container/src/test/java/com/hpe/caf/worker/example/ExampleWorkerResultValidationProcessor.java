package com.hpe.caf.worker.example;

import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.worker.testing.*;
import org.junit.Assert;

/**
 * Processor for validation of the worker result, compares with the expected result in the test item.
 */
public class ExampleWorkerResultValidationProcessor extends ContentResultValidationProcessor<ExampleWorkerResult, ExampleTestInput, ExampleTestExpectation> {

    public ExampleWorkerResultValidationProcessor(WorkerServices workerServices) {
        super(workerServices.getDataStore(), workerServices.getCodec(), ExampleWorkerResult.class, ExampleWorkerResultAccessors::getTextData, SettingsProvider.defaultProvider.getSetting(SettingNames.expectedFolder));
    }

    /**
     * Validates the result by comparing the test expectation in the test item with the actual worker result.
     * First it asserts that the result has the correct worker status.
     * Then it passes the test item and worker result back to the superclass.
     * The superclass compares the referenced data in the worker result with the test item and calculates a similarity percentage
     * between the text in the worker result with the text in the expected result.
     * @param testItem
     * @param message
     * @param workerResult
     * @return boolean
     * @throws Exception
     */
    @Override
    protected boolean processWorkerResult(TestItem<ExampleTestInput, ExampleTestExpectation> testItem, TaskMessage message, ExampleWorkerResult workerResult) throws Exception {
        Assert.assertEquals(testItem.getExpectedOutputData().getResult().workerStatus, workerResult.workerStatus);
        return super.processWorkerResult(testItem, message, workerResult);
    }
}
