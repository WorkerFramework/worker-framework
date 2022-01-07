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
 * This should be called TestInstanceInfo....
 */
public class TestCaseInfo
{
    private String associatedTickets;
    private String comments;
    private String description;
    private String testCaseId;

    /**
     * Getter for property 'testCaseId'.
     *
     * @return Value for property 'testCaseId'.
     */
    public String getTestCaseId()
    {
        return testCaseId;
    }

    /**
     * Setter for property 'testCaseId'.
     *
     * @param testCaseId Value to set for property 'testCaseId'.
     */
    public void setTestCaseId(String testCaseId)
    {
        this.testCaseId = testCaseId;
    }

    /**
     * Getter for property 'associatedTickets'.
     *
     * @return Value for property 'associatedTickets'.
     */
    public String getAssociatedTickets()
    {
        return associatedTickets;
    }

    /**
     * Setter for property 'associatedTickets'.
     *
     * @param associatedTickets Value to set for property 'associatedTickets'.
     */
    public void setAssociatedTickets(String associatedTickets)
    {
        this.associatedTickets = associatedTickets;
    }

    /**
     * Getter for property 'comments'.
     *
     * @return Value for property 'comments'.
     */
    public String getComments()
    {
        return comments;
    }

    /**
     * Setter for property 'comments'.
     *
     * @param comments Value to set for property 'comments'.
     */
    public void setComments(String comments)
    {
        this.comments = comments;
    }

    /**
     * Getter for property 'description'.
     *
     * @return Value for property 'description'.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Setter for property 'description'.
     *
     * @param description Value to set for property 'description'.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }
}
