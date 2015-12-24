package com.hpe.caf.worker.testing;


import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by ploch on 08/11/2015.
 */
public class ExecutionContext {

    private final Signal finishedSignal;
    private final TestItemStore itemStore;
    private final Collection<String> failures = new ArrayList<>();
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


    public void finishedSuccessfully(){
        if (failures.isEmpty()) {
            finishedSignal.doNotify(TestResult.createSuccess());
        }
        else {
            StringBuilder sb = new StringBuilder();
            System.out.println("Tests failed. Number of failures: " + failures.size() + "\nCheck 'Errors.txt' file for details!");
            for (String failure : failures) {
                sb.append(failure).append("\n");
            }
            finishedSignal.doNotify(TestResult.createFailed(sb.toString()));
        }
    }

    public void failed(String message) {
        failures.add(message);
        if (stopOnException) {
            finishedSignal.doNotify(TestResult.createFailed(message));
        }
    }

    public TestResult getTestResult(){
        return finishedSignal.doWait();
    }

}
