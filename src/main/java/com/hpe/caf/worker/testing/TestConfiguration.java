package com.hpe.caf.worker.testing;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Strings;
import com.hpe.caf.worker.testing.configuration.TestCaseFormat;

import java.io.File;

/**
 * Created by ploch on 25/11/2015.
 */
public class TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> {

    public static <TWorkerTask, TWorkerResult, TInput, TExpectation> TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> createDefault(Class<TWorkerTask> workerTaskClass, Class<TWorkerResult> workerResultClass, Class<TInput> inputClass, Class<TExpectation> expectationClass) {
        return createDefault(workerTaskClass, workerResultClass, inputClass, expectationClass, TestCaseFormat.YAML);
    }

    public static <TWorkerTask, TWorkerResult, TInput, TExpectation> TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> createDefault(Class<TWorkerTask> workerTaskClass, Class<TWorkerResult> workerResultClass, Class<TInput> inputClass, Class<TExpectation> expectationClass, TestCaseFormat testCaseFormat) {
        SettingsProvider settingsProvider = SettingsProvider.defaultProvider;

        String useDataStoreSetting = settingsProvider.getSetting(SettingNames.useDataStore);
        boolean useDataStore = useDataStoreSetting != null && Boolean.parseBoolean(useDataStoreSetting);

        ObjectMapper mapper;
        switch (testCaseFormat) {
            case JSON:
                mapper = new ObjectMapper();
                break;
            case XML:
                mapper = new XmlMapper();
                break;
            case YAML:
                mapper = new YAMLMapper();
                break;
            default:
                throw new RuntimeException(String.format("Test case format %s is not supported.", testCaseFormat.toString()));
        }
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new GuavaModule());

        boolean processSubFolders = SettingsProvider.defaultProvider.getBooleanSetting(SettingNames.processSubFolders, true);
        boolean storeTestCaseWithInput = SettingsProvider.defaultProvider.getBooleanSetting(SettingNames.storeTestCaseWithInput, true);
        boolean failOnUnknownProperty = SettingsProvider.defaultProvider.getBooleanSetting(SettingNames.failOnUnknownProperty, true);
        String testSourcefileBaseFolder = settingsProvider.getSetting(SettingNames.testSourcefileBaseFolder);
        String inputFolder = settingsProvider.getSetting(SettingNames.inputFolder);
        String expectedFolder = settingsProvider.getSetting(SettingNames.expectedFolder);

        if (Strings.isNullOrEmpty(inputFolder)) {
            inputFolder = expectedFolder;
        }

        if (!Strings.isNullOrEmpty(inputFolder) && (!inputFolder.endsWith("\\") || !inputFolder.endsWith("/"))) {
            inputFolder = inputFolder + File.separator;
        }

        // Normalize the path of the source files base folder
        testSourcefileBaseFolder = testSourcefileBaseFolder == null ? null : testSourcefileBaseFolder.replace("\\", "/");
        // If the test source file base directory has not been supplied, set the string to be blank
        if (Strings.isNullOrEmpty(testSourcefileBaseFolder)) {
            testSourcefileBaseFolder = "";
        }

        TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration = new TestConfiguration<>(
                expectedFolder, inputFolder, testSourcefileBaseFolder,
                processSubFolders, storeTestCaseWithInput, failOnUnknownProperty,
                useDataStore, settingsProvider.getSetting(SettingNames.dataStoreContainerId),
                mapper,
                workerTaskClass, workerResultClass, inputClass, expectationClass);
        return configuration;
    }

    private String testDataFolder;

    private String testDocumentsFolder;

    private String testSourcefileBaseFolder;

    private boolean useDataStore;

    private String dataStoreContainerId;

    private final ObjectMapper serializer;
    private Class<TWorkerTask> workerTaskClass;

    private Class<TWorkerResult> workerResultClass;

    private Class<TInput> inputClass;

    private Class<TExpectation> expectationClass;

    private boolean processSubFolders;

    private boolean storeTestCaseWithInput;

    private boolean failOnUnknownProperty;

    private TestConfiguration(String testDataFolder, String testDocumentsFolder, String testSourcefileBaseFolder, boolean processSubFolders, boolean storeTestCaseWithInput, boolean failOnUnknownProperty, boolean useDataStore, String dataStoreContainerId, ObjectMapper serializer, Class<TWorkerTask> workerTaskClass, Class<TWorkerResult> workerResultClass, Class<TInput> inputClass, Class<TExpectation> expectationClass) {
        this.testDataFolder = testDataFolder;
        this.testDocumentsFolder = testDocumentsFolder;
        this.testSourcefileBaseFolder = testSourcefileBaseFolder;
        this.processSubFolders = processSubFolders;
        this.storeTestCaseWithInput = storeTestCaseWithInput;
        this.failOnUnknownProperty = failOnUnknownProperty;
        this.useDataStore = useDataStore;
        this.dataStoreContainerId = dataStoreContainerId;
        this.serializer = serializer;
        this.workerTaskClass = workerTaskClass;
        this.workerResultClass = workerResultClass;
        this.inputClass = inputClass;
        this.expectationClass = expectationClass;
    }

    /**
     * Getter for property 'testDataFolder'.
     *
     * @return Value for property 'testDataFolder'.
     */
    public String getTestDataFolder() {
        return testDataFolder;
    }

    /**
     * Getter for property 'testDocumentsFolder'.
     *
     * @return Value for property 'testDocumentsFolder'.
     */
    public String getTestDocumentsFolder() {
        return testDocumentsFolder;
    }

    public String getTestSourcefileBaseFolder() {
        return testSourcefileBaseFolder;
    }

    /**
     * Getter for property 'useDataStore'.
     *
     * @return Value for property 'useDataStore'.
     */
    public boolean isUseDataStore() {
        return useDataStore;
    }

    /**
     * Getter for property 'dataStoreContainerId'.
     *
     * @return Value for property 'dataStoreContainerId'.
     */
    public String getDataStoreContainerId() {
        return dataStoreContainerId;
    }

    /**
     * Getter for property 'workerTaskClass'.
     *
     * @return Value for property 'workerTaskClass'.
     */
    public Class<TWorkerTask> getWorkerTaskClass() {
        return workerTaskClass;
    }

    /**
     * Getter for property 'serializer'.
     *
     * @return Value for property 'serializer'.
     */
    public ObjectMapper getSerializer() {
        return serializer;
    }

    /**
     * Getter for property 'workerResultClass'.
     *
     * @return Value for property 'workerResultClass'.
     */
    public Class<TWorkerResult> getWorkerResultClass() {
        return workerResultClass;
    }

    /**
     * Getter for property 'inputClass'.
     *
     * @return Value for property 'inputClass'.
     */
    public Class<TInput> getInputClass() {
        return inputClass;
    }

    /**
     * Getter for property 'expectationClass'.
     *
     * @return Value for property 'expectationClass'.
     */
    public Class<TExpectation> getExpectationClass() {
        return expectationClass;
    }

    /**
     * Getter for property 'processSubFolders'.
     *
     * @return Value for property 'processSubFolders'.
     */
    public boolean isProcessSubFolders() {
        return processSubFolders;
    }

    /**
     * Getter for property 'storeTestCaseWithInput'.
     *
     * @return Value for property 'storeTestCaseWithInput'.
     */
    public boolean isStoreTestCaseWithInput() {
        return storeTestCaseWithInput;
    }

    /**
     * Getter for property 'failOnUnknownProperty'.
     *
     * @return Value for property 'failOnUnknownProperty'.
     */
    public boolean failOnUnknownProperty() {
        return failOnUnknownProperty;
    }
}
