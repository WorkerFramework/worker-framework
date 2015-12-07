package com.hpe.caf.worker.testing;

/**
 * Created by ploch on 04/12/2015.
 */
public class TestingContext {

    private static TestingContext ourInstance = new TestingContext();

    public static TestingContext getInstance() {
        return ourInstance;
    }

    private TestingContext() {
    }
}
