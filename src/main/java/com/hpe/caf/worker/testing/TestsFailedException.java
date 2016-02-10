package com.hpe.caf.worker.testing;

import java.util.Set;

/**
 * Created by ploch on 05/02/2016.
 */
public class TestsFailedException extends Exception {
    private final Set<TestCaseResult> results;

    public TestsFailedException(String message, Set<TestCaseResult> results) {
        this(message, results, null);
    }

    public TestsFailedException(String message, Set<TestCaseResult> results, Throwable cause) {
        super(message, cause);
        this.results = results;
    }

    /**
     * Getter for property 'results'.
     *
     * @return Value for property 'results'.
     */
    public Set<TestCaseResult> getResults() {
        return results;
    }
}
