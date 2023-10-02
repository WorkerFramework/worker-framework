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
package com.github.workerframework.testworker;

import com.hpe.caf.api.worker.WorkerConfiguration;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

final class TestWorkerConfiguration extends WorkerConfiguration
{
    private String outputQueue;
    private int threads;

    public String getOutputQueue()
    {
        return outputQueue;
    }

    @Min(1)
    @Max(8)
    public int getThreads()
    {
        return threads;
    }

    public void setOutputQueue(final String outputQueue)
    {
        this.outputQueue = outputQueue;
    }

    public void setThreads(final int threads)
    {
        this.threads = threads;
    }
}
