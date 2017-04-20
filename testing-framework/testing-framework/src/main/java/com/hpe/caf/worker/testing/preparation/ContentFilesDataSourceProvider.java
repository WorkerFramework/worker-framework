/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.worker.testing.preparation;

import com.hpe.caf.worker.testing.api.TestDataSource;
import com.hpe.caf.worker.testing.api.TestFailedException;
import com.hpe.caf.worker.testing.storage.TestFileNames;
import com.hpe.caf.worker.testing.util.DirectoryWalker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Created by ploch on 20/04/2017.
 */
public class ContentFilesDataSourceProvider implements Iterable<TestDataSource>
{
    private final Path dataPath;

    public ContentFilesDataSourceProvider(Path dataPath)
    {
        this.dataPath = dataPath;
    }

    @Override
    public Iterator<TestDataSource> iterator()
    {
        try {
            return DirectoryWalker.walk(dataPath, "*", TestFileNames.TEST_FILE_GLOB_FILTER)
                    .filter(path -> !Files.isDirectory(path)).map(path -> createDataSource(path)).iterator();
        }
        catch (IOException e) {
            throw new TestFailedException("Iterating data path '" + dataPath + "' failed.", e);
        }
    }

    private TestDataSource createDataSource(Path file)
    {
        TestDataSource dataSource = new TestDataSource();
        dataSource.addData(file, TestDataSourceIds.CONTENT_FILE);
        return dataSource;
    }
}
