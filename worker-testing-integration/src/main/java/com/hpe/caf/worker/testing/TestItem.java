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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * The {@code TestItem} class represents a single test case. Contains all required data required to create a worker task as well as
 * expectations for the worker result for this particular input.
 *
 * @param <TInput> the test case input type parameter representing all data required to create a worker task
 * @param <TExpected> the test case expectation type parameter representing all data required to validate a worker result (or results)
 */
@JsonPropertyOrder({"tag", "testCaseInformation", "inputData", "expectedData"})
public class TestItem<TInput, TExpected>
{
    private String tag;
    private TestCaseInfo testCaseInformation;

    private TInput inputData;
    private TExpected expectedOutputData;

    @JsonIgnore
    private boolean completed = true;

    @JsonIgnore
    private String inputIdentifier;

    /**
     * Instantiates a new Test item.
     */
    TestItem()
    {
    }

    /**
     * Instantiates a new Test item.
     *
     * @param tag the tag
     * @param inputData the input data
     * @param expectedOutputData the expected output data
     */
    public TestItem(String tag, TInput inputData, TExpected expectedOutputData)
    {

        this.tag = tag;
        this.inputData = inputData;
        this.expectedOutputData = expectedOutputData;
    }

    /**
     * Getter for property 'tag'. Tag represents a test case identifier used (among others) to locate it in {@link TestItemStore}.
     *
     * @return Value for property 'tag'.
     */
    public String getTag()
    {
        return tag;
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
     * Getter for property 'inputData'. Input data should contain all information required to create a worker task.
     *
     * @return Value for property 'inputData'.
     */
    public TInput getInputData()
    {
        return inputData;
    }

    /**
     * Getter for property 'expectedOutputData'. This is the test expectation. It should contain all information required to validate the
     * worker result for particular {@code inputData}.
     *
     * @return Value for property 'expectedOutputData'.
     */
    public TExpected getExpectedOutputData()
    {
        return expectedOutputData;
    }

    /**
     * Getter for property 'completed'. Specifies if this particular test case has been completed. Some workers produce more than one
     * result per one input. Test case is completed only when all expected outputs were processed for this test case. This means that that
     * {@link TExpected} can be composed from more than one expectation.
     *
     * @return Value for property 'completed'. If {@code true} then item is completed and is removed from {@link TestItemStore}.
     */
    @JsonIgnore
    public boolean isCompleted()
    {
        return completed;
    }

    /**
     * Setter for property 'completed'.
     *
     * @param completed Value to set for property 'completed'.
     */
    @JsonIgnore
    public void setCompleted(boolean completed)
    {
        this.completed = completed;
    }

    /**
     * Getter for property 'inputIdentifier'.
     *
     * @return Value for property 'inputIdentifier'.
     */
    @JsonIgnore
    public String getInputIdentifier()
    {
        return inputIdentifier;
    }

    /**
     * Setter for property 'inputIdentifier'.
     *
     * @param inputIdentifier Value to set for property 'inputIdentifier'.
     */
    @JsonIgnore
    public void setInputIdentifier(String inputIdentifier)
    {
        this.inputIdentifier = inputIdentifier;
    }
}
