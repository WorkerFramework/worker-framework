package com.hpe.caf.worker.testing.preparation;

import com.google.common.base.Strings;
import com.hpe.caf.worker.testing.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by ploch on 25/11/2015.
 */
public class PreparationItemProvider<TWorkerTask, TWorkerResult, TInput extends FileTestInputData, TExpectation> extends ContentFilesTestItemProvider {

    private final TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration;

    public PreparationItemProvider(final TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration) {
        super(configuration.getTestDocumentsFolder(), configuration.getTestDataFolder(), "regex:^(?!.*[.](content|testcase)$).*$", true);
        this.configuration = configuration;
    }

    protected TWorkerTask getTaskTemplate() {
        String setting = SettingsProvider.defaultProvider.getSetting(SettingNames.taskTemplate);
        if (Strings.isNullOrEmpty(setting)) {
            System.out.println("Template task not provided, using default.");
            return null;
        }
        System.out.println("Template task file provided: " + setting);
        Path templateTaskFile = Paths.get(setting);
        if (Files.notExists(templateTaskFile)) {
            System.out.println("Provided template file doesn't exist: " + setting);
            throw new AssertionError("Failed to retrieve task template file. File doesn't exist: " + setting);
        }
        try {
            TWorkerTask task = configuration.getSerializer().readValue(templateTaskFile.toFile(), configuration.getWorkerTaskClass());
            return task;
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError("Failed to deserialize template task: " + setting + ". Message: " + e.getMessage());
        }
    }

    @Override
    protected TestItem createTestItem(Path inputFile, Path expectedFile) throws Exception {

        TInput testInput = configuration.getInputClass().newInstance();
        testInput.setUseDataStore(configuration.isUseDataStore());
        testInput.setContainerId(configuration.getDataStoreContainerId());

        Path basePath = Paths.get(getExpectedPath());

        Path relativePath = basePath.relativize(inputFile);

        testInput.setInputFile(relativePath.toString());

        TExpectation testExpectation = configuration.getExpectationClass().newInstance();

        return new TestItem<>(inputFile.getFileName().toString(), testInput, testExpectation);
    }
}