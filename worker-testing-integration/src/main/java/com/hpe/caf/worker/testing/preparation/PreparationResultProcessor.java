/*
 * (c) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hpe.caf.worker.testing.preparation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.worker.testing.*;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by ploch on 25/11/2015.
 */
public class PreparationResultProcessor<TWorkerTask, TWorkerResult, TInput extends FileTestInputData, TExpected> extends OutputToFileProcessor<TWorkerResult, TInput, TExpected> {


    private final TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpected> configuration;

    protected PreparationResultProcessor(final TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpected> configuration, Codec codec) {
        super(codec, configuration.getWorkerResultClass(), configuration.getTestDataFolder());
        this.configuration = configuration;
    }

    /**
     * Getter for property 'configuration'.
     *
     * @return Value for property 'configuration'.
     */
    protected TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpected> getConfiguration() {
        return configuration;
    }

    @Override
    protected byte[] getOutputContent(TWorkerResult workerResult, TaskMessage message, TestItem<TInput, TExpected> testItem) throws Exception {
        return getSerializedTestItem(testItem, configuration);
    }

    @Override
    protected byte[] getFailedOutputContent(TaskMessage message, TestItem<TInput, TExpected> testItem) throws Exception {
        return getSerializedTestItem(testItem, configuration);
    }

    @Override
    protected Path getSaveFilePath(TestItem<TInput, TExpected> testItem, TaskMessage message) {
        Path saveFilePath = super.getSaveFilePath(testItem, message);
        if (configuration.isStoreTestCaseWithInput()) {
            Path fileName = saveFilePath.getFileName();
            Path path = Paths.get(testItem.getInputData().getInputFile());
            saveFilePath = Paths.get(configuration.getTestDataFolder(), path.getParent() == null ? "" : path.getParent().toString(), fileName.toString());
        }
        return saveFilePath;
    }


    protected Path saveContentFile(TestItem<TInput, TExpected> testItem, String baseFileName, String extension, InputStream dataStream) throws IOException {
        String outputFolder = getOutputFolder();
        if (configuration.isStoreTestCaseWithInput()) {

            Path path = Paths.get(testItem.getInputData().getInputFile()).getParent();
            outputFolder =  Paths.get(configuration.getTestDataFolder(), path == null ? "" : path.toString()).toString();
        }

        baseFileName = FilenameUtils.normalize(baseFileName);
        baseFileName = Paths.get(baseFileName).getFileName().toString();
        Path contentFile = Paths.get(outputFolder, baseFileName + "." + extension + ".content");
        Files.deleteIfExists(contentFile);
        Files.copy(dataStream, contentFile, REPLACE_EXISTING);

        return getRelativeLocation(contentFile);
    }

    protected Path getRelativeLocation(Path contentFile) {
        Path relative = Paths.get(configuration.getTestDataFolder()).relativize(contentFile);
        return relative;
    }
}
