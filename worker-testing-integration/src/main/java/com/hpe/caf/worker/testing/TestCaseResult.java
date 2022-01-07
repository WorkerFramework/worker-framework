/*
 * Copyright 2022-2022 Micro Focus or one of its affiliates.
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

/**
 * Created by ploch on 05/02/2016.
 */
public class TestCaseResult
{
    private final TestCaseInfo testCaseInfo;
    private boolean succeeded;
    private String failureMessage;

    public static TestCaseResult createSuccess(TestCaseInfo testCaseInfo)
    {
        return new TestCaseResult(testCaseInfo, true, null);
    }

    public static TestCaseResult createFailure(TestCaseInfo testCaseInfo, String message)
    {
        return new TestCaseResult(testCaseInfo, false, message);
    }

    private TestCaseResult(TestCaseInfo testCaseInfo, boolean succeeded, String failureMessage)
    {
        this.testCaseInfo = testCaseInfo;

        this.succeeded = succeeded;
        this.failureMessage = failureMessage;
    }

    /**
     * Getter for property 'testCaseInfo'.
     *
     * @return Value for property 'testCaseInfo'.
     */
    public TestCaseInfo getTestCaseInfo()
    {
        return testCaseInfo;
    }

    /**
     * Getter for property 'succeeded'.
     *
     * @return Value for property 'succeeded'.
     */
    public boolean isSucceeded()
    {
        return succeeded;
    }

    /**
     * Getter for property 'failureMessage'.
     *
     * @return Value for property 'failureMessage'.
     */
    public String getFailureMessage()
    {
        return failureMessage;
    }

    /**
     * Setter for property 'succeeded'.
     *
     * @param succeeded Value to set for property 'succeeded'.
     */
    public void setSucceeded(boolean succeeded)
    {
        this.succeeded = succeeded;
    }

    /**
     * Setter for property 'failureMessage'.
     *
     * @param failureMessage Value to set for property 'failureMessage'.
     */
    public void setFailureMessage(String failureMessage)
    {
        this.failureMessage = failureMessage;
    }
}
