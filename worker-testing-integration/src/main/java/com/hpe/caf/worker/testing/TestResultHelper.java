package com.hpe.caf.worker.testing;

/**
 * Created by ploch on 25/11/2015.
 */
public class TestResultHelper {

    private TestResultHelper(){}

    public static void testFailed(String message, Exception cause) {
        throw new AssertionError("Test Failed. " + message, cause);
    }

    public static void testFailed(String message)  {
        testFailed(message, null);
    }

    public static void testFailed(TestItem testItem, String message) {
        testFailed(testItem, message, null);
    }

    public static void testFailed(TestItem testItem, String message, Exception cause) {
        testFailed("Test item '" + testItem.getTag() + "' failed. " + message, cause);
    }
}
