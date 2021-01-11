/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.api.worker;

import com.hpe.caf.api.Configuration;

@Configuration
public class WorkerConfiguration
{
    private String workerName;

    private String workerVersion;

    private String rejectQueue;

    public String getWorkerName()
    {
        return workerName;
    }

    public String getWorkerVersion()
    {
        return workerVersion;
    }

    public void setWorkerName(String workerName)
    {
        this.workerName = workerName;
    }

    public void setWorkerVersion(String workerVersion)
    {
        this.workerVersion = workerVersion;
    }

    public String getRejectQueue()
    {
        return rejectQueue == null ? this.workerName + "-reject" : rejectQueue;
    }

    public void setRejectQueue(final String rejectQueue)
    {
        this.rejectQueue = rejectQueue;
    }
}
