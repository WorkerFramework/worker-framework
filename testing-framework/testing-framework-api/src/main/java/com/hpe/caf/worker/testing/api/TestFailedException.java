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
package com.hpe.caf.worker.testing.api;

/**
 * Created by ploch on 07/03/2017.
 */
public class TestFailedException extends RuntimeException {

    private final TestContext testContext;

    public TestContext getTestContext() {
        return testContext;
    }

    public TestFailedException(String message) {
        super(message);
        testContext = null;
    }

    public TestFailedException(String message, Throwable cause) {
        super(message, cause);
        testContext = null;
    }

    public TestFailedException(String message, TestContext testContext) {
        super(message);
        this.testContext = testContext;
    }

    public TestFailedException(String message, Throwable cause, TestContext testContext) {
        super(message, cause);
        this.testContext = testContext;
    }
}
