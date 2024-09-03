/*
 * Copyright 2015-2024 Open Text.
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

import com.hpe.caf.api.Configuration;
import com.hpe.caf.api.worker.WorkerQueueProvider;
import com.hpe.caf.api.worker.WorkerQueueProviderFactory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Configuration
public class TestWorkerQueueProviderFactory implements WorkerQueueProviderFactory
{
    public TestWorkerQueueProviderFactory()
    {
    }

    @NotNull
    @Size(min = 1)
    private String queueImplementation;

    public String getQueueImplementation()
    {
        return queueImplementation;
    }

    public void setQueueImplementation(final String queueImplementation)
    {
        this.queueImplementation = queueImplementation;
    }

    @Override
    public WorkerQueueProvider getQueueProvider()
    {
        return new TestWorkerQueueProvider();
    }
}
