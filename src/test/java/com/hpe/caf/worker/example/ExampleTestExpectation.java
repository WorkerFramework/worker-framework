package com.hpe.caf.worker.example;

import com.hpe.caf.worker.testing.ContentFileTestExpectation;

/**
 * ExampleTestExpectation forms a component of the test item, and contains the expected ExampleWorkerResult, used to compare
 * with the actual worker result.
 */
public class ExampleTestExpectation  extends ContentFileTestExpectation {

    /**
     * ExampleWorkerResult read in from the yaml test case, used to validate the result of the worker is as expected.
     */
    private ExampleWorkerResult result;

    public ExampleTestExpectation() {
    }

    public ExampleWorkerResult getResult() {
        return result;
    }

    public void setResult(ExampleWorkerResult result) {
        this.result = result;
    }
}
