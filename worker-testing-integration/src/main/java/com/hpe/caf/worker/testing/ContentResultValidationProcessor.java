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
package com.hpe.caf.worker.testing;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreSource;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.util.ref.DataSource;
import com.hpe.caf.util.ref.ReferencedData;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Function;
import org.joda.time.DateTime;

/**
 * Created by ploch on 25/11/2015.
 */
public class ContentResultValidationProcessor<TResult, TInput extends FileTestInputData, TExpected extends ContentFileTestExpectation> extends AbstractResultProcessor<TResult, TInput, TExpected>
{

    private final DataStore dataStore;
    private final Function<TResult, ReferencedData> getContentFunc;
    private final String testDataFolder;

    public ContentResultValidationProcessor(final DataStore dataStore, final Codec codec, final Class<TResult> resultClass, final Function<TResult, ReferencedData> getContentFunc, final String testDataFolder)
    {
        super(codec, resultClass);
        this.dataStore = dataStore;
        this.getContentFunc = getContentFunc;
        this.testDataFolder = testDataFolder;
    }

    @Override
    protected boolean processWorkerResult(TestItem<TInput, TExpected> testItem, TaskMessage message, TResult workerResult) throws Exception
    {
        final String func = "Process Worker Result";

        try {
            log(func + " starting....");

            DataSource dataSource = new DataStoreSource(dataStore, getCodec());

            ReferencedData referencedData = getContentFunc.apply(workerResult);

            String contentFileName = testItem.getExpectedOutputData().getExpectedContentFile();
            if (contentFileName != null && contentFileName.length() > 0) {

                log(func + " aquire from source: " + referencedData.getReference() == null ? referencedData.getReference() : "<blob info>");
                InputStream dataStream = referencedData.acquire(dataSource);
                log(func + " aquire from source finished");

                String ocrText = IOUtils.toString(dataStream, StandardCharsets.UTF_8);

                log(func + " got string from source to compare.");

                Path contentFile = Paths.get(contentFileName);
                if (Files.notExists(contentFile)) {
                    contentFile = Paths.get(testDataFolder, contentFileName);
                }
                String expectedOcrText = new String(Files.readAllBytes(contentFile));

                log(func + " got string from expected result to compare.");

                double similarity = ContentComparer.calculateSimilarityPercentage(expectedOcrText, ocrText);

                System.out.println("Test item: " + testItem.getTag() + ". Similarity: " + similarity + "%");
                if (similarity < testItem.getExpectedOutputData().getExpectedSimilarityPercentage()) {
                    TestResultHelper.testFailed(testItem, "Expected similarity of " + testItem.getExpectedOutputData().getExpectedSimilarityPercentage() + "% but actual similarity was " + similarity + "%");
                }
            } else if (referencedData != null) {
                TestResultHelper.testFailed(testItem, "Expected null result.");
            }

            log(func + " returning.");
            return true;
        } finally {
            log(func + " finished....");
        }
    }

    private void log(final String debugInfo)
    {
        System.out.println(DateTime.now().toLocalTime().toString() + debugInfo);
    }
}
