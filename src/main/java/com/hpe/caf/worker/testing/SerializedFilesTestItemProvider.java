package com.hpe.caf.worker.testing;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Strings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by ploch on 24/11/2015.
 */
public class SerializedFilesTestItemProvider<TInput, TExpected> extends ContentFilesTestItemProvider {

    private final Class<TInput> inputClass;
    private final Class<TExpected> expectedClass;
    private final String testSourcefileBaseFolder;
    private final ObjectMapper serializer;

    public SerializedFilesTestItemProvider(TestConfiguration configuration) {
        super(configuration.getTestDocumentsFolder(), configuration.getTestDataFolder(), "glob:*.testcase", configuration.isProcessSubFolders());
        this.inputClass = configuration.getInputClass();
        this.expectedClass = configuration.getExpectationClass();
        this.testSourcefileBaseFolder = configuration.getTestSourcefileBaseFolder();
        serializer = configuration.getSerializer();
        serializer.registerModule(new GuavaModule());
    }

    @Override
    protected TestItem<TInput, TExpected> createTestItem(Path inputFile, Path expectedFile) throws Exception {

        System.out.println("Reading input file: " + inputFile.toString());
        serializer.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        TestItem<TInput, TExpected> item = serializer.readValue(Files.readAllBytes(inputFile), TypeFactory.defaultInstance().constructParametrizedType(TestItem.class, TestItem.class, inputClass, expectedClass));

        // Validate provided file exists
        if (item.getInputData() instanceof FileTestInputData) {
            FileTestInputData data = (FileTestInputData) item.getInputData();
            String sourceFileName = data.getInputFile();
            Path sourceFile = Paths.get(sourceFileName);

            if (Files.notExists(sourceFile) && !Strings.isNullOrEmpty(testSourcefileBaseFolder)) {
                sourceFileName = testSourcefileBaseFolder + data.getInputFile();
                sourceFile = Paths.get(sourceFileName);
            }

            if (Files.notExists(sourceFile)) {
                sourceFileName = getInputPath() + data.getInputFile();
                sourceFile = Paths.get(sourceFileName);
                if (Files.notExists(sourceFile)) {
                    throw new Exception("Could not find input source file " + sourceFile);
                }
            }

            ((FileTestInputData) item.getInputData()).setInputFile(sourceFileName);
        }

        return item;
    }

    @Override
    protected Path getExpectedFile(String expectedPath, String inputFileName) {
        if (inputFileName.endsWith(".xml")) {
            inputFileName = inputFileName.substring(0, inputFileName.length() - 4);
        }
        return super.getExpectedFile(expectedPath, inputFileName);
    }
}
