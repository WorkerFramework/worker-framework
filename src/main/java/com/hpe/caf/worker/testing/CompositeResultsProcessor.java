package com.hpe.caf.worker.testing;

import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;

import java.io.IOException;

/**
 * Created by ploch on 08/11/2015.
 */
public class CompositeResultsProcessor implements ResultProcessor {

    private final ResultProcessor[] processors;

    public CompositeResultsProcessor(ResultProcessor... processors) {

        this.processors = processors;
    }

    @Override
    public boolean process(TestItem testItem, TaskMessage resultMessage) throws CodecException, IOException {
        boolean success = true;
        for (ResultProcessor processor : processors) {
            if (!processor.process(testItem, resultMessage)){
                success = false;
            }
        }
        return success;
    }

    public String getInputIdentifier(TaskMessage message) throws Exception {return "";}
}
