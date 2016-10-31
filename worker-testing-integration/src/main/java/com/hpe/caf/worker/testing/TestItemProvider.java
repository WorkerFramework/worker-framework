package com.hpe.caf.worker.testing;

import java.util.Collection;

/**
 * Represents the contract for a class that provides {@link TestItem} instances describing execution and validation
 * of tests.
 */
public interface TestItemProvider {

    /**
     * Gets items which are then used to create messages for worker-under-test and validate produced result(s).
     *
     * @return the items
     * @throws Exception the exception
     */
    Collection<TestItem> getItems() throws Exception;
}
