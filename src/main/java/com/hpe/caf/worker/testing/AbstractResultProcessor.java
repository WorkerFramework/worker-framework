package com.hpe.caf.worker.testing;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Created by ploch on 08/11/2015.
 */
public abstract class AbstractResultProcessor<TResult, TInput, TExpected> implements ResultProcessor {

    private final Codec codec;
    private final Class<TResult> resultClass;

    /**
     * Getter for property 'codec'.
     *
     * @return Value for property 'codec'.
     */
    protected Codec getCodec() {
        return codec;
    }

    protected AbstractResultProcessor(final Codec codec, final Class<TResult> resultClass) {

        this.codec = codec;
        this.resultClass = resultClass;
    }

    @Override
    public boolean process(TestItem testItem, TaskMessage resultMessage) throws CodecException, IOException {
        TResult workerResult = deserializeMessage(resultMessage, resultClass);
        try {
            return processWorkerResult(testItem, resultMessage, workerResult);
        }
        catch (Throwable e) {
            System.err.println("Failure during processing results. Test item: " + testItem.getTag());
            e.printStackTrace();
            return false;
        }
    }

    protected TResult deserializeMessage(TaskMessage message, Class<TResult> resultClass) throws CodecException {
        TResult workerResult = codec.deserialise(message.getTaskData(), resultClass);
        return workerResult;
    }

    protected abstract boolean processWorkerResult(TestItem<TInput, TExpected> testItem, TaskMessage message, TResult result) throws Exception;

    protected String getMetadataValue(Collection<Map.Entry<String, String>> metadata, String key) {
        String value = "";
        if (metadata != null) {
            for (Map.Entry me : metadata) {
                if (key.equalsIgnoreCase(me.getKey().toString())) {
                    value = me.getValue().toString();
                    break;
                }
            }
        }

        return value;
    }

    protected void clearMetadataValue(Collection<Map.Entry<String, String>> metadata, String key) {
        if (metadata != null) {
            for (Map.Entry me : metadata) {
                if (key.equalsIgnoreCase(me.getKey().toString())) {
                    me.setValue("");
                    break;
                }
            }
        }
    }

    public String getInputIdentifier(TaskMessage message) throws Exception {return "";}
}
