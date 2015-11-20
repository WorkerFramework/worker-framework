package com.hpe.caf.worker.testing;

/**
 * Created by ploch on 07/11/2015.
 */
public class TestItem<TInput, TExpected> {

    private String tag;
    private TInput inputData;
    private TExpected expectedOutputData;

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



}
