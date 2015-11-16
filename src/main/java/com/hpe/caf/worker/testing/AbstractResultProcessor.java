package com.hpe.caf.worker.testing;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;

import java.io.IOException;

/**
 * Created by ploch on 08/11/2015.
 */
public abstract class AbstractResultProcessor<TResult, TInput, TExpected> implements ResultProcessor {

    private final Codec codec;
    private final Class<TResult> resultClass;

    protected AbstractResultProcessor(final Codec codec, final Class<TResult> resultClass) {

        this.codec = codec;
        this.resultClass = resultClass;
    }

    @Override
    public boolean process(TestItem testItem, TaskMessage resultMessage) throws CodecException, IOException {
        TResult workerResult = codec.deserialise(resultMessage.getTaskData(), resultClass);
        try {
            return processWorkerResult(testItem, resultMessage, workerResult);

        }
        catch (Exception ex) {
            return false;
        }
    }

    protected abstract boolean processWorkerResult(TestItem<TInput, TExpected> testItem, TaskMessage message, TResult result) throws IOException;
}
