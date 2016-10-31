package com.hpe.caf.worker.testing.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;


/**
 * The type Test case settings.
 *
 * @param <TWorkerTask>   the type parameter
 * @param <TWorkerResult> the type parameter
 * @param <TInput>        the type parameter
 * @param <TExpectation>  the type parameter
 */
public class TestCaseSettings<TWorkerTask, TWorkerResult, TInput, TExpectation> {

    private DataStoreSettings dataStoreSettings;
    private TestDataSettings testDataSettings;
    private TestCaseClasses<TInput, TExpectation> testCaseClasses;
    private WorkerClasses<TWorkerTask, TWorkerResult> workerClasses;
    private ObjectMapper testCaseSerializer = new YAMLMapper();

    public TestCaseSettings(DataStoreSettings dataStoreSettings, TestDataSettings testDataSettings, TestCaseClasses<TInput, TExpectation> testCaseClasses, WorkerClasses<TWorkerTask, TWorkerResult> workerClasses) {
        this.dataStoreSettings = dataStoreSettings;
        this.testDataSettings = testDataSettings;
        this.testCaseClasses = testCaseClasses;
        this.workerClasses = workerClasses;
    }

    /**
     * Getter for property 'dataStoreSettings'.
     *
     * @return Value for property 'dataStoreSettings'.
     */
    public DataStoreSettings getDataStoreSettings() {
        return dataStoreSettings;
    }

    /**
     * Setter for property 'dataStoreSettings'.
     *
     * @param dataStoreSettings Value to set for property 'dataStoreSettings'.
     */
    public void setDataStoreSettings(DataStoreSettings dataStoreSettings) {
        this.dataStoreSettings = dataStoreSettings;
    }

    /**
     * Getter for property 'folderSettings'.
     *
     * @return Value for property 'folderSettings'.
     */
    public TestDataSettings getTestDataSettings() {
        return testDataSettings;
    }

    /**
     * Setter for property 'folderSettings'.
     *
     * @param testDataSettings Value to set for property 'folderSettings'.
     */
    public void setTestDataSettings(TestDataSettings testDataSettings) {
        this.testDataSettings = testDataSettings;
    }

    /**
     * Getter for property 'testCaseClasses'.
     *
     * @return Value for property 'testCaseClasses'.
     */
    public TestCaseClasses<TInput, TExpectation> getTestCaseClasses() {
        return testCaseClasses;
    }

    /**
     * Setter for property 'testCaseClasses'.
     *
     * @param testCaseClasses Value to set for property 'testCaseClasses'.
     */
    public void setTestCaseClasses(TestCaseClasses<TInput, TExpectation> testCaseClasses) {
        this.testCaseClasses = testCaseClasses;
    }

    /**
     * Getter for property 'workerClasses'.
     *
     * @return Value for property 'workerClasses'.
     */
    public WorkerClasses<TWorkerTask, TWorkerResult> getWorkerClasses() {
        return workerClasses;
    }

    /**
     * Setter for property 'workerClasses'.
     *
     * @param workerClasses Value to set for property 'workerClasses'.
     */
    public void setWorkerClasses(WorkerClasses<TWorkerTask, TWorkerResult> workerClasses) {
        this.workerClasses = workerClasses;
    }

    /**
     * Getter for property 'testCaseSerializer'.
     *
     * @return Value for property 'testCaseSerializer'.
     */
    public ObjectMapper getTestCaseSerializer() {
        return testCaseSerializer;
    }

    /**
     * Setter for property 'testCaseSerializer'.
     *
     * @param testCaseSerializer Value to set for property 'testCaseSerializer'.
     */
    public void setTestCaseSerializer(ObjectMapper testCaseSerializer) {
        this.testCaseSerializer = testCaseSerializer;
    }
}
