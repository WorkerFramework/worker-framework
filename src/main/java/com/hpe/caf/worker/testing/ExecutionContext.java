package com.hpe.caf.worker.testing;


/**
 * Created by ploch on 08/11/2015.
 */
public class ExecutionContext {

    private final Signal finishedSignal;
    private final TestItemStore itemStore;


    public ExecutionContext() {
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
     * Getter for property 'consumerThread'.
     *
     * @return Value for property 'consumerThread'.
     */


    public void finishedSuccessfully(){
        finishedSignal.doNotify(TestResult.createSuccess());
    }

    public void failed(String message) {
        finishedSignal.doNotify(TestResult.createFailed(message));
    }

    public TestResult getTestResult(){
        return finishedSignal.doWait();
    }

}
