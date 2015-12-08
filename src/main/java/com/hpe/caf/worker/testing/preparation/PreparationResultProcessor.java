package com.hpe.caf.worker.testing.preparation;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.worker.testing.OutputToFileProcessor;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;

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
    protected byte[] getOutputContent(TWorkerResult workerResult, TaskMessage message, TestItem<TInput, TExpected> testItem) throws Exception {

        return configuration.getSerializer().writeValueAsBytes(testItem);
    }
}
