package com.hpe.caf.worker.testing;

import java.util.Collection;
import java.util.Set;

/**
 * Created by ploch on 05/02/2016.
 */
public class TestsFailedException extends Exception {
    private final Collection<TestCaseResult> results;

    public TestsFailedException(String message, Collection<TestCaseResult> results) {
        this(message, results, null);
    }

    public TestsFailedException(String message, Collection<TestCaseResult> results, Throwable cause) {
        super(message, cause);
        this.results = results;
    }

    /**
     * Getter for property 'results'.
     *
     * @return Value for property 'results'.
     */
    public Collection<TestCaseResult> getResults() {
        return results;
    }
}
