/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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

import com.google.common.base.Strings;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.util.ref.ReferencedData;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by ploch on 19/11/2015.
 */
public abstract class FileInputWorkerTaskFactory<TTask, TInput extends FileTestInputData, TExpected>
    implements WorkerTaskFactory<TTask, TInput, TExpected>
{
    private final DataStore dataStore;
    private final String containerId;
    private final String testFilesFolder;
    private final String testSourcefileBaseFolder;
    private final String overrideReference;

    public FileInputWorkerTaskFactory(TestConfiguration configuration) throws Exception
    {
        this(WorkerServices.getDefault().getDataStore(), configuration.getDataStoreContainerId(),
             configuration.getTestDataFolder(), configuration.getTestSourcefileBaseFolder(),
             configuration.getOverrideReference());
    }

    public FileInputWorkerTaskFactory(
        DataStore dataStore,
        String containerId,
        String testFilesFolder,
        String testSourcefileBaseFolder,
        String overrideReference
    )
    {
        this.dataStore = dataStore;
        this.containerId = containerId;
        this.testFilesFolder = testFilesFolder;
        this.testSourcefileBaseFolder = testSourcefileBaseFolder;
        this.overrideReference = overrideReference;
    }

    @Override
    public TTask createTask(TestItem<TInput, TExpected> testItem) throws Exception
    {
        ReferencedData sourceData;
        if (!Strings.isNullOrEmpty(overrideReference)) {
            testItem.getInputData().setStorageReference(overrideReference);
            sourceData = ReferencedData.getReferencedData(overrideReference);
        } else if (!Strings.isNullOrEmpty(testItem.getInputData().getStorageReference())) {
            sourceData = ReferencedData.getReferencedData(testItem.getInputData().getStorageReference());
        } else {
            Path inputFile = Paths.get(testItem.getInputData().getInputFile());

            if (Files.notExists(inputFile) && !Strings.isNullOrEmpty(testSourcefileBaseFolder)) {
                inputFile = Paths.get(testSourcefileBaseFolder, testItem.getInputData().getInputFile());
            }

            if (Files.notExists(inputFile)) {
                inputFile = Paths.get(testFilesFolder, testItem.getInputData().getInputFile());
            }

            if (testItem.getInputData().isUseDataStore()) {
                try (InputStream inputStream = Files.newInputStream(inputFile)) {
                    String reference = dataStore.store(inputStream, containerId);
                    sourceData = ReferencedData.getReferencedData(reference);
                    inputStream.close();
                }
            } else {
                byte[] fileContent = Files.readAllBytes(inputFile);
                sourceData = ReferencedData.getWrappedData(fileContent);
            }
        }
        return createTask(testItem, sourceData);
    }

    protected abstract TTask createTask(TestItem<TInput, TExpected> testItem, ReferencedData sourceData);

    /**
     * Getter for property 'containerId'.
     *
     * @return Value for property 'containerId'.
     */
    protected String getContainerId()
    {
        return containerId;
    }
}
