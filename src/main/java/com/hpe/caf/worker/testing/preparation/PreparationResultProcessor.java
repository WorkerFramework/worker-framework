package com.hpe.caf.worker.testing.preparation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.DataStoreSource;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.testing.OutputToFileProcessor;
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
public class PreparationResultProcessor<TWorkerTask, TWorkerResult, TInput, TExpected> extends OutputToFileProcessor<TWorkerResult, TInput, TExpected> {


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
    protected byte[] getOutputContent(TWorkerResult workerResult, TestItem<TInput, TExpected> testItem) throws Exception {
        ObjectMapper mapper = new XmlMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper.writeValueAsBytes(testItem);
    }
}
