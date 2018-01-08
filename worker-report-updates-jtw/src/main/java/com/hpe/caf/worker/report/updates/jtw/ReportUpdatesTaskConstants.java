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

public class ReportUpdatesTaskConstants
{
    /**
     * Identifies the sort of task this message is.
     */
    public static final String REPORT_UPDATES_TASK_NAME = "JobTrackingWorkerReportUpdatesTask";

    /**
     * The numeric API version of the message task.
     */
    public static final int REPORT_UPDATES_TASK_API_VER = 1;

    private ReportUpdatesTaskConstants() { }
}
