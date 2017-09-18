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

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreSource;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.testing.ContentFileTestExpectation;
import com.hpe.caf.worker.testing.FileTestInputData;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Created by ploch on 25/11/2015.
 */
public class ContentResultProcessor<TWorkerTask, TWorkerResult, TInput extends FileTestInputData, TExpected extends ContentFileTestExpectation> extends PreparationResultProcessor<TWorkerTask, TWorkerResult, TInput, TExpected>
{
    private final DataStore dataStore;
    private final Function<TWorkerResult, ReferencedData> getContentFunc;

    protected ContentResultProcessor(TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpected> configuration, Codec codec, DataStore dataStore, Function<TWorkerResult, ReferencedData> getContentFunc)
    {
        super(configuration, codec);
        this.dataStore = dataStore;
        this.getContentFunc = getContentFunc;
    }

    @Override
    protected byte[] getOutputContent(TWorkerResult workerResult, TaskMessage message, TestItem<TInput, TExpected> testItem) throws Exception
    {

        Path contentFile = null;
        ReferencedData textData = getContentFunc.apply(workerResult);
        if (textData != null) {
            InputStream dataStream = textData.acquire(new DataStoreSource(dataStore, getCodec()));

            contentFile = saveContentFile(testItem, testItem.getTag(), "result", dataStream);
        }

        TExpected expectation = testItem.getExpectedOutputData();

        expectation.setExpectedContentFile(contentFile == null ? null : contentFile.toString());
        expectation.setExpectedSimilarityPercentage(80);

        return super.getOutputContent(workerResult, message, testItem);
    }
}
