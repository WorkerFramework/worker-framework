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
import com.hpe.caf.worker.testing.SettingNames;
import com.hpe.caf.worker.testing.SettingsProvider;

/**
 * Created by ploch on 04/12/2015.
 */
public class ConfigurationBuilder<TWorkerTask, TWorkerResult, TInput, TExpectation>
{
    private static SettingsProvider settingsProvider = SettingsProvider.defaultProvider;

    //  private FolderSettings folderSettings = new FolderSettings();
    // private DataStoreSettings dataStoreSettings = new DataStoreSettings();
    //  private final WorkerClasses<TWorkerTask, TWorkerResult> workerClasses;
    //   private final TestCaseClasses<TInput, TExpectation> testCaseClasses;
    private final TestCaseSettings<TWorkerTask, TWorkerResult, TInput, TExpectation> testCaseSettings;

    public ConfigurationBuilder setTestCaseFolder(String folder)
    {
        testCaseSettings.getTestDataSettings().setTestCaseFolder(folder);
        // Default documents folder to the same value as test case
        if (testCaseSettings.getTestDataSettings().getDocumentFolder() == null) {
            testCaseSettings.getTestDataSettings().setDocumentFolder(folder);
        }
        return this;
    }

    public ConfigurationBuilder setDocumentFolder(String folder)
    {
        testCaseSettings.getTestDataSettings().setDocumentFolder(folder);
        return this;
    }

    public ConfigurationBuilder setUseDataStore(boolean useDataStore)
    {
        testCaseSettings.getDataStoreSettings().setUseDataStore(useDataStore);
        return this;
    }

    public ConfigurationBuilder setDataStoreContainerId(String containerId)
    {
        testCaseSettings.getDataStoreSettings().setDataStoreContainerId(containerId);
        return this;
    }

    public ConfigurationBuilder setTestCaseSerializer(ObjectMapper mapper)
    {
        testCaseSettings.setTestCaseSerializer(mapper);
        return this;
    }

    private ConfigurationBuilder(
        Class<TWorkerTask> workerTaskClass,
        Class<TWorkerResult> workerResultClass,
        Class<TInput> testInputClass,
        Class<TExpectation> testExpectationClass)
    {
        testCaseSettings = new TestCaseSettings<>(
            new DataStoreSettings(),
            new TestDataSettings(),
            new TestCaseClasses<>(testInputClass, testExpectationClass),
            new WorkerClasses<>(workerTaskClass, workerResultClass));
    }

    public TestCaseSettings<TWorkerTask, TWorkerResult, TInput, TExpectation> build()
    {
        return testCaseSettings;
    }

    public static <TWorkerTask, TWorkerResult, TInput, TExpectation>
        ConfigurationBuilder<TWorkerTask, TWorkerResult, TInput, TExpectation>
        configure(Class<TWorkerTask> workerTaskClass, Class<TWorkerResult> workerResultClass, Class<TInput> testInputClass, Class<TExpectation> testExpectationClass)
    {
        ConfigurationBuilder<TWorkerTask, TWorkerResult, TInput, TExpectation> builder = new ConfigurationBuilder<>(workerTaskClass, workerResultClass, testInputClass, testExpectationClass);

        //TODO: Temporary solution, will move all config to this package at some point.
        builder.setTestCaseFolder(settingsProvider.getSetting(SettingNames.testCaseFolder))
            .setDocumentFolder(settingsProvider.getSetting(SettingNames.documentFolder))
            .setUseDataStore(settingsProvider.getBooleanSetting(SettingNames.useDataStore, false))
            .setDataStoreContainerId(settingsProvider.getSetting(SettingNames.dataStoreContainerId));

        return builder;
    }

    private static <TWorkerTask, TWorkerResult, TInput, TExpectation> void setDefaults(ConfigurationBuilder<TWorkerTask, TWorkerResult, TInput, TExpectation> builder)
    {
    }
}
