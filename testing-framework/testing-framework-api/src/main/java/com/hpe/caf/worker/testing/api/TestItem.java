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
 * The {@code TestItem} class represents a single test.
 * Contains all required data required to create a worker task
 * as well as expectations for the worker result for this
 * particular input.
 *
 * @param <TInput>       the test case input type parameter representing
 *                       all data required to create a worker task
 * @param <TExpectation> the test case expectation type parameter
 *                       representing all data required to handle a
 *                       worker result (or results)
 */
public class TestItem<TInput, TExpectation>
{

    /**
     * Specifies test item location
     */
    private String location;

    private TestCaseInfo testCaseInformation;

    private TInput inputData;
    private TExpectation expectedOutputData;

    /**
     * Instantiates a new Test item.
     */
    public TestItem()
    {
    }

    /**
     * Instantiates a new Test item.
     *
     * @param testCaseInfo
     * @param inputData          the input data
     * @param expectedOutputData the expected output data
     * @param location
     */
    public TestItem(TestCaseInfo testCaseInfo, TInput inputData, TExpectation expectedOutputData, String location)
    {
        this.testCaseInformation = testCaseInfo;
        this.inputData = inputData;
        this.expectedOutputData = expectedOutputData;
        this.location = location;
    }

    public String getLocation()
    {
        return location;
    }

    /**
     * Getter for property 'testCaseInformation'.
     *
     * @return Value for property 'testCaseInformation'.
     */
    public TestCaseInfo getTestCaseInformation()
    {
        return testCaseInformation;
    }

    /**
     * Setter for property 'testCaseInformation'.
     *
     * @param testCaseInformation Value to set for property 'testCaseInformation'.
     */
    public void setTestCaseInformation(TestCaseInfo testCaseInformation)
    {
        this.testCaseInformation = testCaseInformation;
    }

    /**
     * Getter for property 'inputData'.
     * Input data should contain all information required to create a worker task.
     *
     * @return Value for property 'inputData'.
     */
    public TInput getInputData()
    {
        return inputData;
    }

    public void setInputData(TInput inputData)
    {
        this.inputData = inputData;
    }

    /**
     * Getter for property 'expectedOutputData'.
     * This is the test expectation. It should contain all information required to
     * handle the worker result for particular {@code inputData}.
     *
     * @return Value for property 'expectedOutputData'.
     */
    public TExpectation getExpectedOutputData()
    {
        return expectedOutputData;
    }

    public void setExpectedOutputData(TExpectation expectedOutputData)
    {
        this.expectedOutputData = expectedOutputData;
    }
}
