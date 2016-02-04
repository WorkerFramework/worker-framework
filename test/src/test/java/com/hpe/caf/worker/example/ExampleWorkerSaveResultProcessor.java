package com.hpe.caf.worker.example;

import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.WorkerServices;
import com.hpe.caf.worker.testing.preparation.ContentResultProcessor;

/**
 * Processor for generating testcase and result.content files
 */
public class ExampleWorkerSaveResultProcessor extends ContentResultProcessor<ExampleWorkerTask, ExampleWorkerResult, ExampleTestInput, ExampleTestExpectation> {

    protected ExampleWorkerSaveResultProcessor(TestConfiguration configuration, WorkerServices workerServices){
        super(configuration, workerServices.getCodec(), workerServices.getDataStore(), ExampleWorkerResult::getTextData);
    }

    /**
     * Called by the test app with -g command argument to generate test files.
     * @param exampleWorkerResult
     * @param message
     * @param testItem
     * @return byte[]
     * @throws Exception
     */
    @Override
    protected byte[] getOutputContent(ExampleWorkerResult exampleWorkerResult, TaskMessage message, TestItem<ExampleTestInput, ExampleTestExpectation> testItem) throws Exception {
        testItem.getExpectedOutputData().setResult(exampleWorkerResult);
        return super.getOutputContent(exampleWorkerResult, message, testItem);
    }
}
