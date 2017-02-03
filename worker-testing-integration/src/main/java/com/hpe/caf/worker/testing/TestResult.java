/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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

import java.util.Collection;
import java.util.Set;

/**
 * Created by ploch on 16/11/2015.
 */
public class TestResult {

    private final boolean success;
    private final String errorMessage;
    private final Collection<TestCaseResult> results;

    private TestResult(boolean success, String errorMessage, Collection<TestCaseResult> results) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.results = results;
    }

    public static TestResult createSuccess(Collection<TestCaseResult> results) {
        return new TestResult(true, null, results);
    }

    public static TestResult createFailed(String errorMessage, Collection<TestCaseResult> results) {
        return new TestResult(false, errorMessage, results);
    }

    /**
     * Getter for property 'success'.
     *
     * @return Value for property 'success'.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Getter for property 'errorMessage'.
     *
     * @return Value for property 'errorMessage'.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Getter for property 'results'.
     *
     * @return Value for property 'results'.
     */
    public Collection<TestCaseResult> getResults() {
        return results;
    }

    /* *//**
     * Setter for property 'success'.
     *
     * @param success Value to set for property 'success'.
     *//*
    public void setSuccess(boolean success) {
        this.success = success;
    }*/
}
