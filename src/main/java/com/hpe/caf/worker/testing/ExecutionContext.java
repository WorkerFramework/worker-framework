package com.hpe.caf.worker.testing;


import java.util.*;

/**
 * Created by ploch on 08/11/2015.
 */
public class ExecutionContext {

    private Signal finishedSignal;
    private TestItemStore itemStore;
    private final Map<String, TestCaseResult> results = new HashMap<>();
    private boolean failureEncountered = false;
    private final boolean stopOnException;
    private boolean initialized = false;

    public ExecutionContext(boolean stopOnException) {
        this.stopOnException = stopOnException;
        initializeContext();
    }

    public void initializeContext(){
        finishedSignal = new Signal();
        itemStore = new TestItemStore(this);
        initialized = true;
    }

    /**
     * Getter for property 'isInitialized'.
     *
     * @return Value for property 'isInitialized'.
     */
    public boolean isInitialized() { return initialized; }

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
    public Collection<TestCaseResult> getResults() {
        return results.values();
    }

    public void finishedSuccessfully(){

        if (!failureEncountered) {
            finishedSignal.doNotify(TestResult.createSuccess(results.values()));
        }
        else {
            int failures = 0;
            for (TestCaseResult result : results.values()) {
                if (!result.isSucceeded()) {
                    failures++;
                }
            }
            System.out.println("Tests failed. Number of failures: " + failures);
            finishedSignal.doNotify(TestResult.createFailed("Tests failed. Number of failed test cases: " + failures + ". Number of successful test cases: " + (results.size() - failures), results.values()));
        }
    }

    public void succeeded(TestItem testItem) {
        synchronized (results) {
            results.putIfAbsent(testItem.getTag(), TestCaseResult.createSuccess(testItem.getTestCaseInformation() == null ? createIfNoneProvided(testItem) : testItem.getTestCaseInformation()));
        }
    }

    private TestCaseInfo createIfNoneProvided(TestItem item) {
        TestCaseInfo info = new TestCaseInfo();
        info.setTestCaseId(item.getTag());
        info.setDescription("No description provided!");
        info.setComments("Please update the test case file! Test Case Info was not set!");
        info.setAssociatedTickets("No associated tickets provided!");
        return info;
    }

    public void failed(TestItem testItem, String message) {
        synchronized (results) {
            failureEncountered = true;
            TestCaseResult result = results.get(testItem.getTag());
            if (result == null) {
                results.put(testItem.getTag(), TestCaseResult.createFailure(testItem.getTestCaseInformation() == null ? createIfNoneProvided(testItem) : testItem.getTestCaseInformation(), message));
            }
            else {
                result.setSucceeded(false);
                result.setFailureMessage(result.getFailureMessage() + "\n***\n" + message);
            }
        //    results.add(TestCaseResult.createFailure(testItem.getTestCaseInformation() == null ? createIfNoneProvided(testItem) : testItem.getTestCaseInformation(), message));
        }

        if (stopOnException) {
            finishedSignal.doNotify(TestResult.createFailed(message, results.values()));
        }
    }

    public void testRunsTimedOut() {
        finishedSignal.doNotify(TestResult.createFailed("Tests timed out. Failed.", results.values()));
    }

    public TestResult getTestResult(){
        return finishedSignal.doWait();
    }

}
