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
import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

public final class FileSystemDataStoreHealthcheck implements Callable<HealthResult>
{
    private final Path dataStorePath;

    public FileSystemDataStoreHealthcheck(final Path dataStorePath)
    {
        this.dataStorePath = dataStorePath;
    }

    @Override
    public HealthResult call() throws IOException
    {
        // TODO Need FileVisitOption.FOLLOW_SYMBOLIC_LINK option?
        // try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dataStorePath)) {
        try (final Stream<Path> stream = Files.walk(dataStorePath, 1)) {
            return HealthResult.RESULT_HEALTHY;
        }
    }
}
