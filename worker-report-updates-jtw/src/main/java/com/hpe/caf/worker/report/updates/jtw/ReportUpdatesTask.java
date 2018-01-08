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

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

/**
 * This task is used to inform the Job Tracking worker of the progress of a number of job tasks.
 */
public class ReportUpdatesTask
{
    public ReportUpdatesTask() {
    }

    /**
     * The tracked job tasks whose progress is being reported.
     */
    @NotNull
    private List<ReportUpdatesTaskData> tasks;

    public List<ReportUpdatesTaskData> getTasks() {
        return tasks;
    }

    public void setTasks(final List<ReportUpdatesTaskData> tasks) {
        this.tasks = Objects.requireNonNull(tasks);
    }
}
