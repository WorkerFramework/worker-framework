package com.hpe.caf.worker.testing;

/**
 * Created by ploch on 05/02/2016.
 */
public class TestCaseResult {

    private final TestCaseInfo testCaseInfo;
    private final boolean succeeded;
    private final String failureMessage;

    public static TestCaseResult createSuccess(TestCaseInfo testCaseInfo) {
        return new TestCaseResult(testCaseInfo, true, null);
    }

    public static TestCaseResult createFailure(TestCaseInfo testCaseInfo, String message) {
        return new TestCaseResult(testCaseInfo, false, message);
    }

    private TestCaseResult(TestCaseInfo testCaseInfo, boolean succeeded, String failureMessage) {
        this.testCaseInfo = testCaseInfo;

        this.succeeded = succeeded;
        this.failureMessage = failureMessage;
    }

    /**
     * Getter for property 'testCaseInfo'.
     *
     * @return Value for property 'testCaseInfo'.
     */
    public TestCaseInfo getTestCaseInfo() {
        return testCaseInfo;
    }

    /**
     * Getter for property 'succeeded'.
     *
     * @return Value for property 'succeeded'.
     */
    public boolean isSucceeded() {
        return succeeded;
    }

    /**
     * Getter for property 'failureMessage'.
     *
     * @return Value for property 'failureMessage'.
     */
    public String getFailureMessage() {
        return failureMessage;
    }
}
