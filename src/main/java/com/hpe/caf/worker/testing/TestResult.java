package com.hpe.caf.worker.testing;

/**
 * Created by ploch on 16/11/2015.
 */
public class TestResult {

    private boolean success;
    private final String errorMessage;

    private TestResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static TestResult createSuccess() {
        return new TestResult(true, null);
    }

    public static TestResult createFailed(String errorMessage) {
        return new TestResult(false, errorMessage);
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

    /* *//**
     * Setter for property 'success'.
     *
     * @param success Value to set for property 'success'.
     *//*
    public void setSuccess(boolean success) {
        this.success = success;
    }*/
}
