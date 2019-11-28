/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.worker.testing;

import java.util.*;

/**
 * Created by ploch on 08/11/2015.
 */
public class ExecutionContext
{
    private Signal finishedSignal;
    private TestItemStore itemStore;
    private Map<String, TestCaseResult> results = new HashMap<>();
    private boolean failureEncountered = false;
    private final boolean stopOnException;
    private boolean initialized = false;

    public ExecutionContext(boolean stopOnException)
    {
        this.stopOnException = stopOnException;
    }

    public void initializeContext()
    {
        results = new HashMap<>();
        failureEncountered = false;
        finishedSignal = new Signal();
        itemStore = new TestItemStore(this);
        initialized = true;
    }

    /**
     * Getter for property 'finishedSignal'.
     *
     * @return Value for property 'finishedSignal'.
     */
    public Signal getFinishedSignal()
    {
        verifyInitialized();
        return finishedSignal;
    }

    /**
     * Getter for property 'itemStore'.
     *
     * @return Value for property 'itemStore'.
     */
    public TestItemStore getItemStore()
    {
        verifyInitialized();
        return itemStore;
    }

    /**
     * Getter for property 'results'.
     *
     * @return Value for property 'results'.
     */
    public Collection<TestCaseResult> getResults()
    {
        verifyInitialized();
        return results.values();
    }

    public void verifyInitialized()
    {
        if (initialized == false) {
            throw new RuntimeException("ExecutionContext not initialized");
        }
    }

    public void finishedSuccessfully()
    {
        verifyInitialized();
        if (!failureEncountered) {
            finishedSignal.doNotify(TestResult.createSuccess(results.values()));
        } else {
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

    public void succeeded(TestItem testItem)
    {
        verifyInitialized();
        synchronized (results) {
            results.putIfAbsent(testItem.getTag(), TestCaseResult.createSuccess(testItem.getTestCaseInformation() == null ? createIfNoneProvided(testItem) : testItem.getTestCaseInformation()));
        }
    }

    private TestCaseInfo createIfNoneProvided(TestItem item)
    {
        verifyInitialized();
        TestCaseInfo info = new TestCaseInfo();
        info.setTestCaseId(item.getTag());
        info.setDescription("No description provided!");
        info.setComments("Please update the test case file! Test Case Info was not set!");
        info.setAssociatedTickets("No associated tickets provided!");
        return info;
    }

    public void failed(TestItem testItem, String message)
    {
        verifyInitialized();
        synchronized (results) {
            failureEncountered = true;
            TestCaseResult result = results.get(testItem.getTag());
            if (result == null) {
                results.put(testItem.getTag(), TestCaseResult.createFailure(testItem.getTestCaseInformation() == null ? createIfNoneProvided(testItem) : testItem.getTestCaseInformation(), message));
            } else {
                result.setSucceeded(false);
                result.setFailureMessage(result.getFailureMessage() + "\n***\n" + message);
            }
            //    results.add(TestCaseResult.createFailure(testItem.getTestCaseInformation() == null ? createIfNoneProvided(testItem) : testItem.getTestCaseInformation(), message));
        }

        if (stopOnException) {
            finishedSignal.doNotify(TestResult.createFailed(message, results.values()));
        }
    }

    public void testRunsTimedOut()
    {
        verifyInitialized();
        finishedSignal.doNotify(TestResult.createFailed("Tests timed out. Failed.", results.values()));
    }

    public TestResult getTestResult()
    {
        verifyInitialized();
        return finishedSignal.doWait();
    }
}
