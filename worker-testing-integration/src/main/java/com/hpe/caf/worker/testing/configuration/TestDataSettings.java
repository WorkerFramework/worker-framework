package com.hpe.caf.worker.testing.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by ploch on 02/12/2015.
 */
public class TestDataSettings {

    private String testCaseFolder;
    private String documentFolder;
    private String testCaseExtension;
    private ObjectMapper testCaseSerializer;

    public TestDataSettings(String testCaseFolder, String documentFolder, String testCaseExtension, ObjectMapper testCaseSerializer) {
        this.testCaseFolder = testCaseFolder;
        this.documentFolder = documentFolder;
        this.testCaseExtension = testCaseExtension;
        this.testCaseSerializer = testCaseSerializer;
    }

    public TestDataSettings() {
    }

    /**
     * Getter for property 'testCaseFolder'.
     *
     * @return Value for property 'testCaseFolder'.
     */
    public String getTestCaseFolder() {
        return testCaseFolder;
    }

    /**
     * Setter for property 'testCaseFolder'.
     *
     * @param testCaseFolder Value to set for property 'testCaseFolder'.
     */
    public void setTestCaseFolder(String testCaseFolder) {
        this.testCaseFolder = testCaseFolder;
    }

    /**
     * Getter for property 'documentFolder'.
     *
     * @return Value for property 'documentFolder'.
     */
    public String getDocumentFolder() {
        return documentFolder;
    }

    /**
     * Setter for property 'documentFolder'.
     *
     * @param documentFolder Value to set for property 'documentFolder'.
     */
    public void setDocumentFolder(String documentFolder) {
        this.documentFolder = documentFolder;
    }


}
