package com.hpe.caf.worker.example;

import com.hpe.caf.worker.example.ExampleWorkerTask;
import com.hpe.caf.worker.testing.FileTestInputData;

/**
 * ExampleTestInput is a component of test item, and contains a worker task used to provide test work to a worker.
 */
public class ExampleTestInput extends FileTestInputData {

    /**
     * ExampleWorkerTask read in from the yaml test case and used as an input of test work to the worker.
     */
    private ExampleWorkerTask task;

    public ExampleTestInput() {
    }

    public ExampleWorkerTask getTask() {
        return task;
    }

    public void setTask(ExampleWorkerTask task) {
        this.task = task;
    }
}
