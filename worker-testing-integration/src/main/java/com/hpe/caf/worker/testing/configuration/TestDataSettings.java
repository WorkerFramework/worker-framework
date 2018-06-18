/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.testing.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by ploch on 02/12/2015.
 */
public class TestDataSettings
{
    private String testCaseFolder;
    private String documentFolder;
    private String testCaseExtension;
    private ObjectMapper testCaseSerializer;

    public TestDataSettings(String testCaseFolder, String documentFolder, String testCaseExtension, ObjectMapper testCaseSerializer)
    {
        this.testCaseFolder = testCaseFolder;
        this.documentFolder = documentFolder;
        this.testCaseExtension = testCaseExtension;
        this.testCaseSerializer = testCaseSerializer;
    }

    public TestDataSettings()
    {
    }

    /**
     * Getter for property 'testCaseFolder'.
     *
     * @return Value for property 'testCaseFolder'.
     */
    public String getTestCaseFolder()
    {
        return testCaseFolder;
    }

    /**
     * Setter for property 'testCaseFolder'.
     *
     * @param testCaseFolder Value to set for property 'testCaseFolder'.
     */
    public void setTestCaseFolder(String testCaseFolder)
    {
        this.testCaseFolder = testCaseFolder;
    }

    /**
     * Getter for property 'documentFolder'.
     *
     * @return Value for property 'documentFolder'.
     */
    public String getDocumentFolder()
    {
        return documentFolder;
    }

    /**
     * Setter for property 'documentFolder'.
     *
     * @param documentFolder Value to set for property 'documentFolder'.
     */
    public void setDocumentFolder(String documentFolder)
    {
        this.documentFolder = documentFolder;
    }
}
