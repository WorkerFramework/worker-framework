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
        String normalizedRelativePath = relativePath.toString().replace("\\", "/");

        testInput.setInputFile(normalizedRelativePath);

        TExpectation testExpectation = configuration.getExpectationClass().newInstance();

        return new TestItem<>(normalizedRelativePath, testInput, testExpectation);
    }
}