/*
 * Copyright 2015-2025 Open Text.
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
package com.github.workerframework.core;

import com.github.cafapi.common.api.Codec;
import com.github.cafapi.common.api.CodecException;
import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.api.TaskSourceInfo;
import com.github.workerframework.api.TaskStatus;
import com.github.workerframework.tracking.report.TrackingReport;
import com.github.workerframework.tracking.report.TrackingReportConstants;
import com.github.workerframework.tracking.report.TrackingReportFailure;
import com.github.workerframework.tracking.report.TrackingReportStatus;
import com.github.workerframework.tracking.report.TrackingReportTask;
import com.github.workerframework.util.rabbitmq.RabbitHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TrackingMessageCreator {

    private static final Logger LOG = LoggerFactory.getLogger(WorkerTaskImpl.class);

    private static final boolean isZeroProgressReportingEnabled
        = !Boolean.parseBoolean(System.getenv("CAF_WORKER_DISABLE_ZERO_PROGRESS_REPORTING"));

    /**
     * Used to create a task message to publish a progress report update message onto the tracking pipe.
     */
    public static TaskMessage createTrackingMessage(
        final List<TaskMessage> reportUpdates,
        final String correlationId,
        final Map<String, Object> headers,
        final Codec codec
    )
    {
        //  If nothing to report then do nothing.
        if (reportUpdates == null || reportUpdates.isEmpty()) {
            return null;
        }

        //  Make a note of the tracking pipe where progress report updates are to be sent.
        final String trackingPipe = reportUpdates.get(0).getTracking().getTrackingPipe();

        //  Build up a TrackingReportTask comprising a list of progress report updates to send.
        final TrackingReportTask trackingReportTask = createReportUpdatesTask(reportUpdates, headers);
        if (trackingReportTask.trackingReports.isEmpty()) {
            return null;
        }

        //  Serialise the list of progress report updates to send.
        final byte[] reportUpdatesTaskData;
        try {
            reportUpdatesTaskData = codec.serialise(trackingReportTask);
        } catch (final CodecException e) {
            LOG.error("Failed to serialise report update task data.");
            throw new RuntimeException(e);
        }

        //  Create a task message comprising the progress report updates.
        return new TaskMessage(
            UUID.randomUUID().toString(), TrackingReportConstants.TRACKING_REPORT_TASK_NAME,
            TrackingReportConstants.TRACKING_REPORT_TASK_API_VER, reportUpdatesTaskData, TaskStatus.NEW_TASK,
            Collections.<String, byte[]>emptyMap(), trackingPipe, null, null, correlationId);
    }

    private static TrackingReportTask createReportUpdatesTask(
        final List<TaskMessage> taskMessages,
        final Map<String, Object> headers
    ) {

        final List<TrackingReport> trackingReports = new ArrayList<>();

        //  Iterate through each task message and generate a progress report update.
        for (final TaskMessage tm : taskMessages) {
            //  Create a new instance of TrackingReport to hold the progress report update data.
            final TrackingReport trackingReport = new TrackingReport();

            //  Set job task identifier.
            trackingReport.jobTaskId = tm.getTracking().getJobTaskId();

            //  Get task status.
            final TaskStatus taskStatus = tm.getTaskStatus();

            //  Check task status to determine if task is to be reported as complete or not.
            if (taskStatus == TaskStatus.NEW_TASK || taskStatus == TaskStatus.RESULT_SUCCESS ||
                taskStatus == TaskStatus.RESULT_FAILURE) {
                final String trackToPipe = tm.getTracking().getTrackTo();
                final String toPipe = tm.getTo();

                if ((toPipe == null && trackToPipe == null) || (trackToPipe != null &&
                    trackToPipe.equalsIgnoreCase(toPipe))) {
                    //  Task should be reported as complete.
                    trackingReport.status = TrackingReportStatus.Complete;
                } else if (isZeroProgressReportingEnabled) {
                    //  Task should be reported as in progress.
                    trackingReport.status = TrackingReportStatus.Progress;
                    trackingReport.estimatedPercentageCompleted = 0;
                } else {
                    continue;
                }
            } else if (taskStatus == TaskStatus.RESULT_EXCEPTION || taskStatus == TaskStatus.INVALID_TASK) {
                //  Failed to execute job task. Configure failure details to be reported.
                final TrackingReportFailure failure = new TrackingReportFailure();
                failure.failureId= taskStatus.toString();
                failure.failureTime = new Date();
                failure.failureSource = getWorkerName(tm);
                final byte[] taskData = tm.getTaskData();
                if (taskData != null) {
                    failure.failureMessage = new String(taskData, StandardCharsets.UTF_8);
                }
                trackingReport.failure = failure;

                //  Task should be reported as rejected.
                trackingReport.status = TrackingReportStatus.Failed;
            } else {
                //  TODO
                //  NOTE - this logic has been copied across from JobTrackingWorkerFactory->reportProxiedTask but
                //  I cannot see how we fall into this code given all TaskStatus enumerations have been evaluated by now
                //  and TaskStatus appears to be non-nullable given annotation specified in the TaskMessage class.

                //  Check for rejected headers.
                final boolean rejected =
                    headers.getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED, null) != null;
                final int retries =
                    Integer.parseInt(String.valueOf(headers.getOrDefault(
                        RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, "0")));

                if (rejected) {
                    final String rejectedHeader = String.valueOf(headers.get(
                        RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED));
                    final String rejectionDetails =
                        MessageFormat.format("{0}. Execution of this job task was retried {1} times.",
                            rejectedHeader, retries);

                    //  Configure failure details to be reported.
                    final TrackingReportFailure failure = new TrackingReportFailure();
                    failure.failureId = RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED;
                    failure.failureTime = new Date();
                    failure.failureSource = getWorkerName(tm);
                    failure.failureMessage = rejectionDetails;
                    trackingReport.failure = failure;

                    //  Task should be reported as rejected.
                    trackingReport.status = TrackingReportStatus.Failed;
                } else {
                    trackingReport.retries = retries;

                    //  Task should be reported as retry.
                    trackingReport.status = TrackingReportStatus.Retry;
                }
            }

            //  Add tracking report to list.
            trackingReports.add(trackingReport);
        }

        //  Build up TrackingReportTask data to send to tracking pipe.
        final TrackingReportTask trackingReportTask = new TrackingReportTask();
        trackingReportTask.trackingReports = trackingReports;

        return trackingReportTask;
    }

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
