package com.hpe.caf.worker.example;

import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.testing.FileInputWorkerTaskFactory;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;

/**
 * Task factory for creating tasks from test item.
 */
public class ExampleWorkerTaskFactory extends FileInputWorkerTaskFactory<ExampleWorkerTask, ExampleTestInput, ExampleTestExpectation> {
    public ExampleWorkerTaskFactory(TestConfiguration configuration) throws Exception {
        super(configuration);
    }

    /**
     * Creates a task from a test item (the test item is generated from the yaml test case).
     * @param testItem
     * @param sourceData
     * @return ExampleWorkerTask
     */
    @Override
    protected ExampleWorkerTask createTask(TestItem<ExampleTestInput, ExampleTestExpectation> testItem, ReferencedData sourceData) {
        ExampleWorkerTask task = testItem.getInputData().getTask();

        //setting task source data to the source data parameter.
        task.sourceData = sourceData;
        task.datastorePartialReference = testItem.getInputData().getTask().datastorePartialReference;
        task.action = testItem.getInputData().getTask().action;

        return task;
    }

    @Override
    public String getWorkerName() {
        return ExampleWorkerConstants.WORKER_NAME;
    }

    @Override
    public int getApiVersion() {
        return ExampleWorkerConstants.WORKER_API_VER;
    }
}
