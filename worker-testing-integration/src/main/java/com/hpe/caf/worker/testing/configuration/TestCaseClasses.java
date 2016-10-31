package com.hpe.caf.worker.testing.configuration;

/**
 * Created by ploch on 04/12/2015.
 */
public class TestCaseClasses<TInput, TExpectation> {

    private Class<TInput> inputClass;

    private Class<TExpectation> expectationClass;

    public TestCaseClasses(Class<TInput> inputClass, Class<TExpectation> expectationClass) {
        this.inputClass = inputClass;
        this.expectationClass = expectationClass;
    }

    /**
     * Getter for property 'inputClass'.
     *
     * @return Value for property 'inputClass'.
     */
    public Class<TInput> getInputClass() {
        return inputClass;
    }

    /**
     * Getter for property 'expectationClass'.
     *
     * @return Value for property 'expectationClass'.
     */
    public Class<TExpectation> getExpectationClass() {
        return expectationClass;
    }
}
