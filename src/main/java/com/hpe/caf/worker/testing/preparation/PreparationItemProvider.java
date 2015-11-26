package com.hpe.caf.worker.testing.preparation;

import com.hpe.caf.worker.testing.ContentFilesTestItemProvider;
import com.hpe.caf.worker.testing.FileTestInputData;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by ploch on 25/11/2015.
 */
public class PreparationItemProvider<TWorkerTask, TWorkerResult, TInput extends FileTestInputData, TExpectation> extends ContentFilesTestItemProvider {

    private final TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration;

    public PreparationItemProvider(final TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpectation> configuration) {
        super(configuration.getTestDocumentsFolder(), configuration.getTestDataFolder(), "*", true);
        this.configuration = configuration;
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