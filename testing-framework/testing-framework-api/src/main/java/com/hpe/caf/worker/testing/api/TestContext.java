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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ploch on 07/03/2017.
 */
public class TestContext
{

    private final TestItem currentTestItem;

    private final CompletionSignal signal;
    /*private TestResult testResult = new TestResult();*/
    private final List<ValidationResult> validationResults = new ArrayList<>();
    private Object workerTask;

    public TestContext(TestItem currentTestItem, CompletionSignal signal)
    {
        this.currentTestItem = currentTestItem;
        this.signal = signal;
    }

    public Object getWorkerTask()
    {
        return workerTask;
    }

    public void setWorkerTask(Object workerTask)
    {
        this.workerTask = workerTask;
    }

    public List<ValidationResult> getValidationResults()
    {
        return validationResults;
    }

    public TestItem getCurrentTestItem()
    {
        return currentTestItem;
    }

    public void notifyCompleted()
    {
        signal.doNotifyCompleted();
    }

    /*public TestResult getTestResult() {
        return testResult;
    }*/
}
