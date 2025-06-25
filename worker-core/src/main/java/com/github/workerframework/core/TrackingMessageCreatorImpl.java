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

import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.api.TaskSourceInfo;
import com.github.workerframework.api.TaskStatus;
import com.github.workerframework.api.TrackingInfo;
import com.github.workerframework.api.TrackingMessageCreator;
import com.github.workerframework.api.WorkerConfiguration;
import com.github.workerframework.api.WorkerResponse;
import com.github.workerframework.tracking.report.TrackingReportConstants;
import com.google.common.base.MoreObjects;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public enum TrackingMessageCreatorImpl implements TrackingMessageCreator {

    INSTANCE;

    private static final String WORKER_VERSION_UNKNOWN = "UNKNOWN";

    @Override
    public TaskMessage createResponseTaskMessage(
        final TaskMessage taskMessage,
        final WorkerResponse response,
        final Map<String, byte[]> responseContext,
        final TrackingInfo trackingInfo,
        final WorkerConfiguration workerConfig
    ) {
        return new TaskMessage(
            taskMessage.getTaskId(),
            response.getMessageType(),
            response.getApiVersion(),
            response.getData(),
            response.getTaskStatus(),
            responseContext,
            response.getQueueReference(),
            trackingInfo,
            new TaskSourceInfo(getWorkerName(workerConfig, response.getMessageType()), getWorkerVersion(workerConfig)),
            taskMessage.getCorrelationId());
    }

    @Override
    public TaskMessage createInvalidTaskMessage(
        final TaskMessage taskMessage,
        final String message,
        final String routingKey,
        final WorkerConfiguration workerConfig) {
        final var taskClassifier = MoreObjects.firstNonNull(taskMessage.getTaskClassifier(), "");
        final byte[] taskData = message == null ? new byte[]{} : message.getBytes(StandardCharsets.UTF_8);
        if (taskMessage == null) {
            return createInvalidTaskMessage(taskData, routingKey);
        }
        return new TaskMessage(
            MoreObjects.firstNonNull(taskMessage.getTaskId(), ""),
            taskClassifier,
            taskMessage.getTaskApiVersion(),
            taskData,
            TaskStatus.INVALID_TASK,
            MoreObjects.firstNonNull(taskMessage.getContext(), Collections.emptyMap()),
            routingKey,
            taskMessage.getTracking(),
            new TaskSourceInfo(getWorkerName(workerConfig, taskClassifier), getWorkerVersion(workerConfig)),
            taskMessage.getCorrelationId());
    }

    private TaskMessage createInvalidTaskMessage(
        byte[] taskData,
        final String routingKey
    ) {
        return new TaskMessage(
            UUID.randomUUID().toString(),
            TrackingReportConstants.TRACKING_REPORT_TASK_NAME,
            TrackingReportConstants.TRACKING_REPORT_TASK_API_VER,
            taskData,
            TaskStatus.INVALID_TASK,
            Collections.emptyMap(),
            routingKey);
    }

    @Override
    public TaskMessage createReportUpdateMessage(
        final String correlationId,
        final byte[] reportUpdatesTaskData,
        final String routingKey
    ) {
        return new TaskMessage(
            UUID.randomUUID().toString(),
            TrackingReportConstants.TRACKING_REPORT_TASK_NAME,
            TrackingReportConstants.TRACKING_REPORT_TASK_API_VER,
            reportUpdatesTaskData,
            TaskStatus.NEW_TASK,
            Collections.emptyMap(),
            routingKey,
            null,
            null,
            correlationId);
    }

    private String getWorkerName(final WorkerConfiguration workerConfig, final String defaultName)
    {
        if (workerConfig != null) {
            final String workerName = workerConfig.getWorkerName();

            if (workerName != null) {
                return workerName;
            }
        }

        return defaultName;
    }

    private String getWorkerVersion(final WorkerConfiguration workerConfig)
    {
        if (workerConfig != null) {
            final String workerVersion = workerConfig.getWorkerVersion();

            if (workerVersion != null) {
                return workerVersion;
            }
        }

        return WORKER_VERSION_UNKNOWN;
    }
}
