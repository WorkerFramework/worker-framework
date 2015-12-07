package com.hpe.caf.worker.testing;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by ploch on 07/11/2015.
 */
public class TestItem<TInput, TExpected> {

    private String tag;
    private TInput inputData;
    private TExpected expectedOutputData;

    @JsonIgnore
    private boolean completed = true;

    TestItem(){}

    public TestItem(String tag, TInput inputData, TExpected expectedOutputData) {

        this.tag = tag;
        this.inputData = inputData;
        this.expectedOutputData = expectedOutputData;
    }

    /**
     * Getter for property 'tag'.
     *
     * @return Value for property 'tag'.
     */
    public String getTag() {
        return tag;
    }

    /**
     * Getter for property 'inputData'.
     *
     * @return Value for property 'inputData'.
     */
    public TInput getInputData() {
        return inputData;
    }

    /**
     * Getter for property 'expectedOutputData'.
     *
     * @return Value for property 'expectedOutputData'.
     */
    public TExpected getExpectedOutputData() {
        return expectedOutputData;
    }

    /**
     * Getter for property 'completed'.
     *
     * @return Value for property 'completed'.
     */
    @JsonIgnore
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Setter for property 'completed'.
     *
     * @param completed Value to set for property 'completed'.
     */
    @JsonIgnore
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
