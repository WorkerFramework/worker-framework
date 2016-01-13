package com.hpe.caf.worker.testing;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The {@code TestItem} class contains all required data required to create
 * a worker task as well as expectations regarding the worker result for this
 * particular input.
 * {@code TestItem} is a self-contained <b>test case</b>. It includes all the input
 * data and all data required to validate the result.
 *
 * @param <TInput>    the type parameter
 * @param <TExpected> the type parameter
 */
public class TestItem<TInput, TExpected> {

    private String tag;
    private TInput inputData;
    private TExpected expectedOutputData;

    @JsonIgnore
    private boolean completed = true;

    @JsonIgnore
    private String inputIdentifier;

    /**
     * Instantiates a new Test item.
     */
    TestItem(){}

    /**
     * Instantiates a new Test item.
     *
     * @param tag                the tag
     * @param inputData          the input data
     * @param expectedOutputData the expected output data
     */
    public TestItem(String tag, TInput inputData, TExpected expectedOutputData) {

        this.tag = tag;
        this.inputData = inputData;
        this.expectedOutputData = expectedOutputData;
    }

    /**
     * Getter for property 'tag'.
     * Tag represents a test case identifier used (among others) to locate it in {@link TestItemStore}.
     *
     * @return Value for property 'tag'.
     */
    public String getTag() {
        return tag;
    }

    /**
     * Getter for property 'inputData'.
     * Input data should contain all information required to create a worker task.
     *
     * @return Value for property 'inputData'.
     */
    public TInput getInputData() {
        return inputData;
    }

    /**
     * Getter for property 'expectedOutputData'.
     * This is the test expectation. It should contain all information required to
     * validate the worker result for particular {@code inputData}.
     *
     * @return Value for property 'expectedOutputData'.
     */
    public TExpected getExpectedOutputData() {
        return expectedOutputData;
    }

    /**
     * Getter for property 'completed'. Specifies if this particular test case has been completed.
     * Some workers produce more than one result per one input. Test case is completed only when all
     * expected outputs were processed for this test case. This means that that {@link TExpected} can
     * be composed from more than one expectation.
     *
     * @return Value for property 'completed'. If {@code true} then item is completed and is removed
     * from {@link TestItemStore}.
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

    /**
     * Getter for property 'inputIdentifier'.
     *
     * @return Value for property 'inputIdentifier'.
     */
    @JsonIgnore
    public String getInputIdentifier() {
        return inputIdentifier;
    }

    /**
     * Setter for property 'inputIdentifier'.
     *
     * @param inputIdentifier Value to set for property 'inputIdentifier'.
     */
    @JsonIgnore
    public void setInputIdentifier(String inputIdentifier) {
        this.inputIdentifier = inputIdentifier;
    }
}
