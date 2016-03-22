package com.hpe.caf.worker.testing;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * The {@code TaskMessageFactory} class creates messages for publishing to the worker queue.
 */
public class TaskMessageFactory {

    private final java.lang.String CONTEXT_KEY = "context";
    private final byte[] CONTEXT_DATA = "testData".getBytes(StandardCharsets.UTF_8);
    private final Codec codec;
    private final String workerName;
    private final String workerInputQueueName;
    private final int apiVersion;

    /**
     * Instantiates a new Task message factory.
     *
     * @param codec      the codec
     * @param workerName the worker name
     * @param apiVersion the api version
     */
    public TaskMessageFactory(final Codec codec, final String workerName, final String workerInputQueueName, final int apiVersion) {

        this.codec = codec;
        this.workerName = workerName;
        this.workerInputQueueName = workerInputQueueName;
        this.apiVersion = apiVersion;
    }

    /**
     * Create task message.
     *
     * @param workerTask the worker task
     * @param taskId     the task id
     * @return the task message
     * @throws CodecException the codec exception
     */
    public TaskMessage create(final Object workerTask, final String taskId) throws CodecException {

        Map<java.lang.String, byte[]> context = Collections.singletonMap(CONTEXT_KEY, CONTEXT_DATA);

        //TODO - Cannot include tracking info yet because it will cause response messages to be diverted to trackingPipe (Job Tracking Worker, see CAF-600).
        //TrackingInfo tracking = new TrackingInfo("J23.1.1", new Date(), "http://thehost:1234/job-service/v1/jobs/23/isActive", "test-tracking-1", "test-tracking-end-1");
        TrackingInfo tracking = null;

        TaskMessage msg = new TaskMessage(taskId, workerName, apiVersion, codec.serialise(workerTask), TaskStatus.NEW_TASK, context, workerInputQueueName, tracking);
        return msg;
    }
}
