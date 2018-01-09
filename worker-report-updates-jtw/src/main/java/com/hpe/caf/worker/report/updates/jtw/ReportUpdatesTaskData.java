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
package com.hpe.caf.worker.report.updates.jtw;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Objects;

/**
 * Holds task data for a progress report update.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportUpdatesTaskData
{
    /**
     * The identifier of the tracked job task whose progress is being reported.
     */
    @NotNull
    private String taskId;

    /**
     * Specifies how the job task should be reported.
     */
    @NotNull
    private ReportUpdatesStatus reportUpdatesStatus;

    /**
     * The tracked job task's estimated percentage complete.
     */
    @Min(0)
    @Max(100)
    private int estimatedPercentageCompleted;

    /**
     * The number of attempted retries for the job task.
     */
    private int retries;

    /**
     * The failure id in the event of an invalid task or exception.
     */
    private String failureId;

    /**
     * The time of failure in the event of an invalid task or exception.
     */
    private Date failureTime;

    /**
     * The source of failure in the event of an invalid task or exception.
     */
    private String failureSource;

    /**
     * The failure message in the event of an invalid task or exception.
     */
    private String failureMessage;

    public String getJobTaskId() {
        return taskId;
    }

    public void setJobTaskId(final String taskId) {
        this.taskId = Objects.requireNonNull(taskId);
    }

    public ReportUpdatesStatus getReportUpdatesStatus() {
        return reportUpdatesStatus;
    }

    public void setReportUpdatesStatus(final ReportUpdatesStatus reportUpdatesStatus) {
        this.reportUpdatesStatus = reportUpdatesStatus;
    }

    public int getEstimatedPercentageCompleted() {
        return estimatedPercentageCompleted;
    }

    public void setEstimatedPercentageCompleted(final int estimatedPercentageCompleted) {
        this.estimatedPercentageCompleted = estimatedPercentageCompleted;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(final int retries) {
        this.retries = retries;
    }

    public String getFailureId() {
        return failureId;
    }

    public void setFailureId(final String failureId) {
        this.failureId = failureId;
    }

    public Date getFailureTime() {
        return failureTime;
    }

    public void setFailureTime(final Date failureTime) {
        this.failureTime = failureTime;
    }

    public String getFailureSource() {
        return failureSource;
    }

    public void setFailureSource(final String failureSource) {
        this.failureSource = Objects.requireNonNull(failureSource);
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(final String failureMessage) {
        this.failureMessage = Objects.requireNonNull(failureMessage);
    }
}
