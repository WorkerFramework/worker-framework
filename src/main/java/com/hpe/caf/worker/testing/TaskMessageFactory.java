package com.hpe.caf.worker.testing;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Created by ploch on 07/11/2015.
 */
public class TaskMessageFactory {

    private final java.lang.String CONTEXT_KEY = "context";
    private final byte[] CONTEXT_DATA = "testData".getBytes(StandardCharsets.UTF_8);
    private final Codec codec;
    private final String workerName;
    private final int apiVersion;

    public TaskMessageFactory(final Codec codec, final String workerName, final int apiVersion) {

        this.codec = codec;
        this.workerName = workerName;
        this.apiVersion = apiVersion;
    }

    public TaskMessage create(final Object workerTask, final String taskId) throws CodecException {

        Map<java.lang.String, byte[]> context = Collections.singletonMap(CONTEXT_KEY, CONTEXT_DATA);
        TaskMessage msg = new TaskMessage(taskId, workerName, apiVersion, codec.serialise(workerTask), TaskStatus.NEW_TASK, context);

        return msg;
    }
}
