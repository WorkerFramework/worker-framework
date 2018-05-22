/*
 * Copyright 2018-2017 EntIT Software LLC, a Micro Focus company.
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

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * This task is used to report on the progress of a number of tracked job tasks.
 */
public final class TrackingReportTask
{
    /**
     * The tracked job tasks whose progress is being reported.
     */
    @NotNull
    public List<TrackingReport> trackingReports;
}
