/*
 * Copyright 2015-2023 Open Text.
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
package com.hpe.caf.worker.tracking.report;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Holds tracking report data.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TrackingReport
{
    /**
     * The identifier of the tracked job task whose progress is being reported.
     */
    @NotNull
    public String jobTaskId;

    /**
     * Specifies how the job task should be reported.
     */
    @NotNull
    public TrackingReportStatus status;

    /**
     * The tracked job task's estimated percentage complete.
     */
    @Min(0)
    @Max(100)
    public int estimatedPercentageCompleted;

    /**
     * The number of attempted retries for the tracked job task.
     */
    public Integer retries;

    /**
     * The failure details in the event of an invalid job task or exception.
     */
    public TrackingReportFailure failure;
}
