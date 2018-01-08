/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.worker.core;

import com.google.common.base.MoreObjects;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.naming.ServicePath;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;
import com.hpe.caf.util.rabbitmq.RabbitHeaders;
import com.hpe.caf.worker.report.updates.jtw.ReportUpdatesStatus;
import com.hpe.caf.worker.report.updates.jtw.ReportUpdatesTask;
import com.hpe.caf.worker.report.updates.jtw.ReportUpdatesTaskConstants;
import com.hpe.caf.worker.report.updates.jtw.ReportUpdatesTaskData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WorkerTaskImpl implements WorkerTask
{
    private static final String WORKER_VERSION_UNKNOWN = "UNKNOWN";
    private static final Logger LOG = LoggerFactory.getLogger(WorkerTaskImpl.class);

    private final ServicePath servicePath;
    private final WorkerCallback workerCallback;
    private final WorkerFactory workerFactory;
    private final String messageId;
    private final TaskMessage taskMessage;
    private final MessagePriorityManager priorityManager;
    private int responseCount;
    private final Object responseCountLock;
    private final SingleResponseMessageBuffer singleMessageBuffer;
    private final ProgressReportBuffer progressReportBuffer;
    private final AtomicInteger currentSubtaskId;
    private final Semaphore subtasksPublishedSemaphore;
    private final boolean poison;
    private Codec codec = null;
    private Map<String, Object> headers;

    public WorkerTaskImpl(
            final ServicePath servicePath,
            final WorkerCallback workerCallback,
            final WorkerFactory workerFactory,
            final String messageId,
            final TaskMessage taskMessage,
            final boolean poison,
            final Map<String, Object> headers,
            final MessagePriorityManager priorityManager
    )
    {
        this.servicePath = servicePath;
        this.workerCallback = workerCallback;
        this.workerFactory = workerFactory;
        this.messageId = messageId;
        this.taskMessage = taskMessage;
        this.priorityManager = Objects.requireNonNull(priorityManager);
        this.responseCount = 0;
        this.responseCountLock = new Object();
        this.singleMessageBuffer = new SingleResponseMessageBuffer();
        this.progressReportBuffer = new ProgressReportBuffer();
        this.currentSubtaskId = new AtomicInteger();
        this.subtasksPublishedSemaphore = new Semaphore(0);
        this.poison = poison;
        this.headers = headers;

        try {
            codec = ModuleLoader.getService(Codec.class);
        } catch (final ModuleLoaderException e) {
            LOG.error("Failed to load Codec module.");
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getClassifier()
    {
        return taskMessage.getTaskClassifier();
    }

    @Override
    public int getVersion()
    {
        return taskMessage.getTaskApiVersion();
    }

    @Override
    public TaskStatus getStatus()
    {
        return taskMessage.getTaskStatus();
    }

    @Override
    public byte[] getData()
    {
        return taskMessage.getTaskData();
    }

    @Override
    public byte[] getContext()
    {
        return taskMessage.getContext().get(servicePath.toString());
    }

    @Override
    public TrackingInfo getTrackingInfo()
    {
        return taskMessage.getTracking();
    }

    @Override
    public TaskSourceInfo getSourceInfo()
    {
        return taskMessage.getSourceInfo();
    }

    @Override
    public void addResponse(final WorkerResponse response, final boolean includeTaskContext)
    {
        Objects.requireNonNull(response);
        Objects.requireNonNull(response.getQueueReference());

        incrementResponseCount(false);

        final TaskMessage responseMessage = createResponseMessage(includeTaskContext, response);

        singleMessageBuffer.add(responseMessage);

        //  Add report update task message to progress report buffer.
        progressReportBuffer.add(responseMessage, headers);
    }

    @Override
    public void sendMessage(final TaskMessage tm)
    {
        Objects.requireNonNull(tm);

        // Publish this message
        workerCallback.send("-1", tm);
    }

    @Override
    public void setResponse(final WorkerResponse response)
    {
        Objects.requireNonNull(response);

        incrementResponseCount(true);

        final TaskMessage responseMessage = createResponseMessage(true, response);

        //  Add report update task message to progress report buffer.
        progressReportBuffer.add(responseMessage, headers);

        completeResponse(responseMessage);
    }

    private TaskMessage createResponseMessage(final boolean includeTaskContext, final WorkerResponse response)
    {
        final Map<String, byte[]> responseContext = createFullResponseContext(includeTaskContext, response.getContext());

        final String responseMessageType = response.getMessageType();

        //  Check if a tracking change is required. If empty string then no further changes required.
        final TrackingInfo trackingInfo;
        if ("".equals(response.getTrackTo())) {
            //  No tracking changes required.
            trackingInfo = taskMessage.getTracking();
        } else {
            //  Set trackTo field to the specified value.
            trackingInfo = getTrackingInfoWithChanges(response.getTrackTo());
        }

        final TaskMessage responseMessage = new TaskMessage(
            taskMessage.getTaskId(), responseMessageType,
            response.getApiVersion(), response.getData(),
            response.getTaskStatus(), responseContext,
            response.getQueueReference(), trackingInfo,
            new TaskSourceInfo(getWorkerName(responseMessageType), getWorkerVersion()));
        responseMessage.setPriority(priorityManager.getResponsePriority(taskMessage));

        return responseMessage;
    }

    private TrackingInfo getTrackingInfoWithChanges(final String trackTo) {
        TrackingInfo trackingInfo = null;

        final TrackingInfo taskMessageTracking = taskMessage.getTracking();
        if (taskMessageTracking != null) {
            trackingInfo = new TrackingInfo(taskMessageTracking);
            trackingInfo.setTrackTo(trackTo);
        }

        return trackingInfo;
    }

    @Override
    public void setResponse(final TaskRejectedException taskRejectedException)
    {
        incrementResponseCount(true);

        LOG.info("Worker requested to abandon task {} (message id: {})",
                 taskMessage.getTaskId(), taskMessage, taskRejectedException);

        workerCallback.abandon(messageId, taskRejectedException);
    }

    @Override
    public void setResponse(final InvalidTaskException invalidTaskException)
    {
        if (invalidTaskException == null) {
            throw new IllegalArgumentException();
        }

        incrementResponseCount(true);

        LOG.error("Task data is invalid for {}, returning status {}",
                  taskMessage.getTaskId(), TaskStatus.INVALID_TASK, invalidTaskException);

        final String taskClassifier = MoreObjects.firstNonNull(taskMessage.getTaskClassifier(), "");

        final String invalidTaskExceptionMessage = invalidTaskException.getMessage();
        final byte[] taskData
            = invalidTaskExceptionMessage == null
                ? new byte[]{} : invalidTaskExceptionMessage.getBytes(StandardCharsets.UTF_8);

        final Map<String, byte[]> context = MoreObjects.firstNonNull(
            taskMessage.getContext(),
            Collections.<String, byte[]>emptyMap());

        final TaskMessage invalidResponse = new TaskMessage(
            MoreObjects.firstNonNull(taskMessage.getTaskId(), ""),
            taskClassifier,
            taskMessage.getTaskApiVersion(),
            taskData,
            TaskStatus.INVALID_TASK,
            context,
            workerFactory.getInvalidTaskQueue(),
            taskMessage.getTracking(),
            new TaskSourceInfo(getWorkerName(taskClassifier), getWorkerVersion()));

        //  Add report update task message to progress report buffer.
        progressReportBuffer.add(invalidResponse, headers);

        completeResponse(invalidResponse);
    }

    public Worker createWorker()
        throws InvalidTaskException, TaskRejectedException
    {
        return workerFactory.getWorker(this);
    }

    public void logInterruptedException(final InterruptedException interruptedException)
    {
        LOG.warn("Worker interrupt signalled, not performing callback for task {} (message id: {})",
                 taskMessage.getTaskId(), messageId, interruptedException);
    }

    public boolean isResponseSet()
    {
        return (responseCount < 0);
    }

    /**
     * Checks that the response count hasn't been finalised, and adds one to it if it hasn't been.
     */
    private void incrementResponseCount(final boolean isFinalResponse)
    {
        synchronized (responseCountLock) {
            final int rc = responseCount + 1;
            if (rc <= 0) {
                throw new RuntimeException("Final response already set!");
            }

            responseCount = isFinalResponse ? -rc : rc;
        }
    }

    /**
     * Returns the final response count if it has been established, or throws an exception if it hasn't.
     */
    private int getFinalResponseCount()
    {
        final int rc = responseCount;
        if (rc >= 0) {
            throw new RuntimeException("Final response count not yet known!");
        }

        return -rc;
    }

    public boolean isPoison()
    {
        return poison;
    }

    private String getWorkerName(final String defaultName)
    {
        final com.hpe.caf.api.worker.WorkerConfiguration workerConfig = workerFactory.getWorkerConfiguration();

        if (workerConfig != null) {
            final String workerName = workerConfig.getWorkerName();

            if (workerName != null) {
                return workerName;
            }
        }

        return defaultName;
    }

    private String getWorkerVersion()
    {
        final com.hpe.caf.api.worker.WorkerConfiguration workerConfig = workerFactory.getWorkerConfiguration();

        if (workerConfig != null) {
            final String workerVersion = workerConfig.getWorkerVersion();

            if (workerVersion != null) {
                return workerVersion;
            }
        }

        return WORKER_VERSION_UNKNOWN;
    }

    /**
     * Creates a copy of the context (optionally), and updates it with the specified entry if it is non-null.
     */
    private Map<String, byte[]> createFullResponseContext(
        final boolean includeTaskContext,
        final byte[] responseContext
    )
    {
        final Map<String, byte[]> context = includeTaskContext
            ? new HashMap<>(taskMessage.getContext())
            : new HashMap<>();

        if (responseContext != null) {
            context.put(servicePath.toString(), responseContext);
        }

        return context;
    }

    /**
     * Holds a single message back from being published so that it could potentially be made into the final completion message (although
     * the functionality to do that is not written yet).
     */
    private final class SingleResponseMessageBuffer
    {
        private TaskMessage buffer;
        private final Object bufferLock;
        private boolean isBuffering;

        public SingleResponseMessageBuffer()
        {
            this.buffer = null;
            this.bufferLock = new Object();
            this.isBuffering = true;
        }

        /**
         * Add a new message into the buffer, publishing any message that is currently in it to make room.
         */
        public void add(final TaskMessage newResponse)
        {
            Objects.requireNonNull(newResponse);
            addOrFlush(newResponse);
        }

        /**
         * Flush the buffer and turn it off. Any subsequent calls to add() will not be buffered.
         */
        public void flush()
        {
            isBuffering = false;
            addOrFlush(null);
        }

        private void addOrFlush(final TaskMessage newResponse)
        {
            final TaskMessage previousResponse;
            final boolean newResponseBuffered;

            // Get the previous message out of the buffer,
            // and if buffering is still enabled then put the new message into it
            synchronized (bufferLock) {
                previousResponse = buffer;
                buffer = (newResponseBuffered = isBuffering) ? newResponse : null;
            }

            // Publish the previous message if there is one
            publishSubtask(previousResponse);

            // Publish the new message if it was not buffered
            if (!newResponseBuffered) {
                // Append the subtask id to the task id
                publishSubtask(newResponse);
            }
        }
    }

    /**
     * Holds a message along with any headers stamped on the message. This is used to publish report updates.
     */
    private final class TaskMessageAndHeaders
    {
        private TaskMessage message;
        private Map<String, Object> headers;

        public TaskMessageAndHeaders(final TaskMessage message, final Map<String, Object> headers)
        {
            this.message = message;
            this.headers = headers;
        }

        public TaskMessage getTaskMessage() {
            return message;
        }

        public Map<String, Object> getHeaders() {
            return headers;
        }
    }

    /**
     * Holds the progress report update messages that needs to be forwarded on to the tracking pipe.
     */
    private final class ProgressReportBuffer
    {
        private List<TaskMessageAndHeaders> buffer;
        private final Object bufferLock;
        private boolean isBuffering;
        private int bufferLimit;

        public ProgressReportBuffer()
        {
            this.buffer = new ArrayList<>();
            this.bufferLock = new Object();
            this.isBuffering = true;

            final String reportUpdatesBufferLimit = System.getenv("CAF_WORKER_REPORT_UPDATES_BUFFER_LIMIT");
            if (null == reportUpdatesBufferLimit) {
                // Default buffer limit to 5 if the environment variable is not present.
                this.bufferLimit = 5;
            } else {
                this.bufferLimit = Integer.parseInt(reportUpdatesBufferLimit);
            }
        }

        /**
         * Add a new message along with any headers stamped on the message onto the buffer.
         */
        public void add(final TaskMessage taskMessage, final Map<String, Object> headers)
        {
            Objects.requireNonNull(taskMessage);
            Objects.requireNonNull(headers);

            //  Ignore if no tracking information and tracking pipe is available.
            if (taskMessage.getTracking() != null &&
                    taskMessage.getTracking().getTrackingPipe() != null &&
                    !taskMessage.getTracking().getTrackingPipe().isEmpty()) {
                addOrFlush(taskMessage, headers);
            }
        }

        /**
         * Flush the buffer and turn it off. Any subsequent calls to add() will not be buffered.
         */
        public void flush()
        {
            isBuffering = false;
            addOrFlush(null, null);
        }

        private void addOrFlush(final TaskMessage taskMessage, final Map<String, Object> headers)
        {
            List<TaskMessageAndHeaders> bufferContentsToPublish = null;

            //  If buffering is enabled, then group the supplied message and corresponding header details together.
            TaskMessageAndHeaders msgAndHeaders = null;
            if (isBuffering){
                msgAndHeaders = new TaskMessageAndHeaders(taskMessage, headers);
            }

            synchronized (bufferLock) {
                //  If buffer limit has exceeded, then it needs to be flushed.
                if ((buffer.size() >= bufferLimit) || (!isBuffering)) {
                    bufferContentsToPublish = new ArrayList<>(buffer);
                    buffer.clear();
                }

                //  If buffering is enabled, then add message and corresponding headers to the buffer if it does not
                //  already exist.
                if (isBuffering && (!buffer.contains(msgAndHeaders))) {
                    buffer.add(msgAndHeaders);
                }
            }

            //  Publish the messages currently in the buffer to be published.
            if (bufferContentsToPublish != null && bufferContentsToPublish.size() > 0) {
                publishReportUpdates(bufferContentsToPublish);
            }
        }
    }

    /**
     * Gets the next subtask identifier, appends it to the message and publishes it.
     */
    private void publishSubtask(final TaskMessage responseMessage)
    {
        // Do not do anything if the message is null
        if (responseMessage == null) {
            return;
        }

        // Get the next subtask identifier
        final int subtaskId = currentSubtaskId.incrementAndGet();

        // Append the subtask id to the task id
        updateTaskId(responseMessage, subtaskId, false);

        // Publish this message
        workerCallback.send(messageId, responseMessage);

        // Allow the final task to complete (in case it was blocking waiting on the message to be published)
        subtasksPublishedSemaphore.release();
    }

    /**
     * Finalises the final response message and publishes it (after any subtasks).
     */
    private void completeResponse(final TaskMessage responseMessage)
    {
        // Get the final number of responses
        final int finalResponseCount = getFinalResponseCount();

        // Check if there are multiple responses
        if (finalResponseCount > 1) {
            // Ensure the buffer is flushed
            singleMessageBuffer.flush();

            // Add a suffix the task id
            updateTaskId(responseMessage, finalResponseCount, true);

            // Ensure that all the subtasks have been published before continuing
            subtasksPublishedSemaphore.acquireUninterruptibly(finalResponseCount - 1);
        }

        //  Ensure all report updates have been sent.
        progressReportBuffer.flush();

        // Complete the task
        workerCallback.complete(messageId, responseMessage.getTo(), responseMessage);
    }

    /**
     * Updates the specified {@link TaskMessage} with the specified subtask identifier.
     */
    private void updateTaskId(final TaskMessage responseMessage, final int subtaskId, final boolean isFinalResponse)
    {
        // Put together the suffix to be added
        final String subtaskSuffix;
        {
            final StringBuilder builder = new StringBuilder();
            builder.append('.');
            builder.append(subtaskId);
            if (isFinalResponse) {
                builder.append('*');
            }

            subtaskSuffix = builder.toString();
        }

        // Update the task id
        responseMessage.setTaskId(taskMessage.getTaskId() + subtaskSuffix);

        // Update the tracking info
        final TrackingInfo taskMessageTracking = taskMessage.getTracking();
        if (taskMessageTracking != null) {
            final String trackingTaskId = taskMessageTracking.getJobTaskId();

            if (trackingTaskId != null) {
                final TrackingInfo trackingInfo = new TrackingInfo(taskMessageTracking);
                trackingInfo.setJobTaskId(trackingTaskId + subtaskSuffix);

                responseMessage.setTracking(trackingInfo);
            }
        }
    }

    /**
     * Used to publish progress report update messages onto the tracking pipe.
     */
    private void publishReportUpdates(final List<TaskMessageAndHeaders> reportUpdates)
    {
        //  Make a note of the tracking pipe where progress report updates are to be sent.
        final String trackingPipe = getTrackingPipe(reportUpdates);

        //  Build up a ReportUpdatesTask comprising a list of progress report updates to send.
        final ReportUpdatesTask reportUpdatesTask = getReportUpdatesTask(reportUpdates);
        if (reportUpdatesTask.getTasks() == null || reportUpdatesTask.getTasks().size() == 0) {
            return;
        }

        //  Serialise the list of progress report updates to send.
        final byte[] reportUpdatesTaskData;
        try {
            reportUpdatesTaskData = codec.serialise(reportUpdatesTask);
        } catch (final CodecException e) {
            LOG.error("Failed to serialise report update task data.");
            throw new RuntimeException(e);
        }

        //  Create a task message comprising the progress report updates.
        final TaskMessage responseMessage = new TaskMessage(
                UUID.randomUUID().toString(), ReportUpdatesTaskConstants.REPORT_UPDATES_TASK_NAME,
                ReportUpdatesTaskConstants.REPORT_UPDATES_TASK_API_VER, reportUpdatesTaskData, TaskStatus.NEW_TASK,
                Collections.<String, byte[]>emptyMap(), trackingPipe);

        //  Publish the task message comprising the report updates.
        workerCallback.reportUpdate(messageId, trackingPipe, responseMessage);

        // TODO - do we need a semaphore here?
    }

    private String getTrackingPipe(final List<TaskMessageAndHeaders> taskMessageAndHeaders)
    {
        //  Return the first tracking pipe. All task messages are expected to comprise the same
        //  tracking pipe.
        return taskMessageAndHeaders.get(0).getTaskMessage().getTracking().getTrackingPipe();
    }

    private ReportUpdatesTask getReportUpdatesTask(final List<TaskMessageAndHeaders> taskMessageAndHeaders) {

        //  Build up ReportUpdatesTask data to send to tracking pipe.
        final ReportUpdatesTask reportUpdatesTask = new ReportUpdatesTask();
        final List<ReportUpdatesTaskData> reportUpdatesTaskDataList = new ArrayList<>();

        //  Iterate through each task message and corresponding headers to generate a progress report update.
        for (final TaskMessageAndHeaders msgAndHeaders : taskMessageAndHeaders) {
            //  Retrieve message and header details.
            final TaskMessage tm = msgAndHeaders.getTaskMessage();
            final Map<String, Object> headers = msgAndHeaders.getHeaders();

            //  Create a new instance of ReportUpdatesTaskData to hold the progress report update data.
            final ReportUpdatesTaskData reportUpdatesTaskData = new ReportUpdatesTaskData();

            //  Set job task identifier.
            reportUpdatesTaskData.setJobTaskId(tm.getTracking().getJobTaskId());

            //  Get task status.
            final TaskStatus taskStatus = tm.getTaskStatus();

            //  Check task status to determine if task is to be reported as complete or not.
            if (taskStatus == TaskStatus.NEW_TASK || taskStatus == TaskStatus.RESULT_SUCCESS || taskStatus == TaskStatus.RESULT_FAILURE) {
                final String trackToPipe = tm.getTracking().getTrackTo();
                final String toPipe = tm.getTo();

                if ((toPipe == null && trackToPipe == null) || (trackToPipe != null && trackToPipe.equalsIgnoreCase(toPipe))) {
                    //  Task should be reported as complete.
                    reportUpdatesTaskData.setReportUpdatesStatus(ReportUpdatesStatus.Complete);
                } else {
                    //  Task should be reported as in progress.
                    reportUpdatesTaskData.setReportUpdatesStatus(ReportUpdatesStatus.Progress);
                    reportUpdatesTaskData.setEstimatedPercentageCompleted(0);
                }
            } else if (taskStatus == TaskStatus.RESULT_EXCEPTION || taskStatus == TaskStatus.INVALID_TASK) {
                //  Failed to execute job task. Configure failure details to be reported.
                reportUpdatesTaskData.setFailureId(taskStatus.toString());
                reportUpdatesTaskData.setFailureTime(new Date());
                reportUpdatesTaskData.setFailureSource(getWorkerName(tm));
                final byte[] taskData = tm.getTaskData();
                if (taskData != null) {
                    reportUpdatesTaskData.setFailureMessage(new String(taskData, StandardCharsets.UTF_8));
                }
                //  Task should be reported as rejected.
                reportUpdatesTaskData.setReportUpdatesStatus(ReportUpdatesStatus.Rejected);
            } else {
                //  Check for rejected headers.
                final boolean rejected = headers.getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED, null) != null;
                final int retries = Integer.parseInt(String.valueOf(headers.getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, "0")));
                if (rejected) {
                    final String rejectedHeader = String.valueOf(headers.get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED));
                    final String rejectionDetails = MessageFormat.format("{0}. Execution of this job task was retried {1} times.", rejectedHeader, retries);

                    //  Configure failure details to be reported.
                    reportUpdatesTaskData.setFailureId(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED);
                    reportUpdatesTaskData.setFailureTime(new Date());
                    reportUpdatesTaskData.setFailureSource(getWorkerName(tm));
                    reportUpdatesTaskData.setFailureMessage(rejectionDetails);

                    //  Task should be reported as rejected.
                    reportUpdatesTaskData.setReportUpdatesStatus(ReportUpdatesStatus.Rejected);
                } else {
                    reportUpdatesTaskData.setRetries(retries);

                    //  Task should be reported as retry.
                    reportUpdatesTaskData.setReportUpdatesStatus(ReportUpdatesStatus.Retry);
                }
            }

            //  Add ReportUpdatesTaskData to list.
            reportUpdatesTaskDataList.add(reportUpdatesTaskData);
        }

        // Add progress report update task data to ReportUpdatesTask and return.
        if (reportUpdatesTaskDataList.size() > 0) {
            reportUpdatesTask.setTasks(reportUpdatesTaskDataList);
        }

        return reportUpdatesTask;
    }

    /**
     * Returns the worker name from the source information in the task message or an "Unknown" string if it is not present.
     *
     * @param taskMessage the task message to be examined
     * @return the name of the worker that created the task message
     */
    private static String getWorkerName(final TaskMessage taskMessage)
    {
        final TaskSourceInfo sourceInfo = taskMessage.getSourceInfo();
        if (sourceInfo == null) {
            return "Unknown - no source info";
        }

        final String workerName = sourceInfo.getName();
        if (workerName == null) {
            return "Unknown - worker name not set";
        }

        return workerName;
    }

}
