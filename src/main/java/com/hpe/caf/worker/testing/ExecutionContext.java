package com.hpe.caf.worker.testing;


import java.util.*;

/**
 * Created by ploch on 08/11/2015.
 */
public class ExecutionContext {

    private final Signal finishedSignal;
    private final TestItemStore itemStore;
    private final Set<TestCaseResult> results = new HashSet<>();
    private boolean failureEncountered = false;
    private final boolean stopOnException;

    public ExecutionContext(boolean stopOnException) {
        this.stopOnException = stopOnException;
        finishedSignal = new Signal();
        itemStore = new TestItemStore(this);
    }

    /**
     * Getter for property 'finishedSignal'.
     *
     * @return Value for property 'finishedSignal'.
     */
    public Signal getFinishedSignal() {
        return finishedSignal;
    }

    /**
     * Getter for property 'itemStore'.
     *
     * @return Value for property 'itemStore'.
     */
    public TestItemStore getItemStore() {
        return itemStore;
    }

    /**
     * Getter for property 'results'.
     *
     * @return Value for property 'results'.
     */
    public Set<TestCaseResult> getResults() {
        return results;
    }

    public void finishedSuccessfully(){

        if (!failureEncountered) {
            finishedSignal.doNotify(TestResult.createSuccess(results));
        }
        else {
            int failures = 0;
            for (TestCaseResult result : results) {
                if (!result.isSucceeded()) {
                    failures++;
                }
            }
            System.out.println("Tests failed. Number of failures: " + failures);
            finishedSignal.doNotify(TestResult.createFailed("Tests failed. Number of failed test cases: " + failures + ". Number of successful test cases: " + (results.size() - failures), results));
        }
    }

    public void succeeded(TestItem testItem) {
        synchronized (results) {
            results.add(TestCaseResult.createSuccess(testItem.getTestCaseInformation() == null ? createIfNoneProvided(testItem) : testItem.getTestCaseInformation()));
        }
    }

    private TestCaseInfo createIfNoneProvided(TestItem item) {
        TestCaseInfo info = new TestCaseInfo();
        info.setTestCaseId("Unknown. Test item tag is: " + item.getTag());
        info.setDescription("No description provided!");
        info.setComments("Please update the test case file! Test Case Info was not set!");
        info.setAssociatedTickets("No associated tickets provided!");
        return info;
    }

    public void failed(TestItem testItem, String message) {
        synchronized (results) {
            failureEncountered = true;
            results.add(TestCaseResult.createFailure(testItem.getTestCaseInformation() == null ? createIfNoneProvided(testItem) : testItem.getTestCaseInformation(), message));
        }

        if (stopOnException) {
            finishedSignal.doNotify(TestResult.createFailed(message, results));
        }
    }

    public void testRunsTimedOut() {
        finishedSignal.doNotify(TestResult.createFailed("Tests timed out. Failed.", results));
    }

    public TestResult getTestResult(){
        return finishedSignal.doWait();
    }

}
