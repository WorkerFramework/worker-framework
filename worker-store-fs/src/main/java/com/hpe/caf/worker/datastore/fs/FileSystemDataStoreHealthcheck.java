/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.datastore.fs;

import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import java.nio.file.*;
import java.util.concurrent.Callable;

public final class FileSystemDataStoreHealthcheck implements Callable<HealthResult>
{
    private final Path dataStorePath;

    public FileSystemDataStoreHealthcheck(final Path dataStorePath)
    {
        this.dataStorePath = dataStorePath;
    }

    @Override
    public HealthResult call() throws Exception
    {
        if (Files.exists(dataStorePath)) {
            return HealthResult.RESULT_HEALTHY;
        } else {
            return new HealthResult(
                HealthStatus.UNHEALTHY,
                String.format("Unable to access data store directory: %s", dataStorePath.toString()));
        }
    }
}
