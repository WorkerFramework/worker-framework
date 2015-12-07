package com.hpe.caf.worker.testing;

/**
 * Created by ploch on 16/11/2015.
 */
public class Signal {

    private TestResult testResult;

    private static class MonitorObject {}

    MonitorObject myMonitorObject = new MonitorObject();
    boolean wasSignalled = false;

    public TestResult doWait(){
        synchronized(myMonitorObject){
            while(!wasSignalled){
                try{
                    myMonitorObject.wait();
                } catch(InterruptedException e){
                    return TestResult.createFailed(e.getMessage());
                }
            }
            wasSignalled = false;
            return testResult;
        }
    }

    public void doNotify(TestResult testResult){
        synchronized(myMonitorObject){
            if (this.testResult == null) {
                this.testResult = testResult;
            }
            wasSignalled = true;
            myMonitorObject.notify();
        }
    }
}
