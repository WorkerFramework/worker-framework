/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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

import java.util.Date;

/**
 * Holds the tracked job task failure details in the event of an invalid task or exception.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TrackingReportFailure
{
    /**
     * The failure id in the event of an invalid task or exception.
     */
    public String failureId;

    /**
     * The time of failure in the event of an invalid task or exception.
     */
    public Date failureTime;

    /**
     * The source of failure in the event of an invalid task or exception.
     */
    public String failureSource;

    /**
     * The failure message in the event of an invalid task or exception.
     */
    public String failureMessage;
}
