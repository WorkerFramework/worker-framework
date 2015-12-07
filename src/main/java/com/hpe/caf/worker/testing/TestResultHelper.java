package com.hpe.caf.worker.testing;

/**
 * Created by ploch on 25/11/2015.
 */
public class TestResultHelper {

    private TestResultHelper(){}

    public static void testFailed(String message)  {
        throw new AssertionError("Test Failed. " + message);
    }

    public static void testFailed(TestItem testItem, String message) {
        testFailed("Test item '" + testItem.getTag() + "' failed. " + message);
    }
}
