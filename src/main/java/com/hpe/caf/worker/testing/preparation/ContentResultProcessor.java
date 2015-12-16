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
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.function.Function;

/**
 * Created by ploch on 25/11/2015.
 */
public class ContentResultProcessor<TWorkerTask, TWorkerResult, TInput extends FileTestInputData, TExpected extends ContentFileTestExpectation> extends PreparationResultProcessor<TWorkerTask, TWorkerResult, TInput, TExpected> {
    private final DataStore dataStore;
    private final Function<TWorkerResult, ReferencedData> getContentFunc;

    protected ContentResultProcessor(TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpected> configuration, Codec codec, DataStore dataStore, Function<TWorkerResult, ReferencedData> getContentFunc) {
        super(configuration, codec);
        this.dataStore = dataStore;
        this.getContentFunc = getContentFunc;
    }

    @Override
    protected byte[] getOutputContent(TWorkerResult workerResult, TaskMessage message, TestItem<TInput, TExpected> testItem) throws Exception {

        Path contentFile = null;
        ReferencedData textData = getContentFunc.apply(workerResult);
        if (textData != null) {
            InputStream dataStream = textData.acquire(new DataStoreSource(dataStore, getCodec()));

            contentFile = saveContentFile(testItem, testItem.getTag(), "result", dataStream );
        }

        TExpected expectation = testItem.getExpectedOutputData();

        expectation.setExpectedContentFile(contentFile == null ? null : contentFile.toString());
        expectation.setExpectedSimilarityPercentage(80);

        return super.getOutputContent(workerResult, message, testItem);
    }
}
