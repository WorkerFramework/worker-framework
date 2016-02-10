package com.hpe.caf.worker.testing;

import java.util.Set;

/**
 * Created by ploch on 16/11/2015.
 */
public class TestResult {

    private final boolean success;
    private final String errorMessage;
    private final Set<TestCaseResult> results;

    private TestResult(boolean success, String errorMessage, Set<TestCaseResult> results) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.results = results;
    }

    public static TestResult createSuccess(Set<TestCaseResult> results) {
        return new TestResult(true, null, results);
    }

    public static TestResult createFailed(String errorMessage, Set<TestCaseResult> results) {
        return new TestResult(false, errorMessage, results);
    }

    /**
     * Getter for property 'success'.
     *
     * @return Value for property 'success'.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Getter for property 'errorMessage'.
     *
     * @return Value for property 'errorMessage'.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Getter for property 'results'.
     *
     * @return Value for property 'results'.
     */
    public Set<TestCaseResult> getResults() {
        return results;
    }

    /* *//**
     * Setter for property 'success'.
     *
     * @param success Value to set for property 'success'.
     *//*
    public void setSuccess(boolean success) {
        this.success = success;
    }*/
}
