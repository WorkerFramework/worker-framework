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
package com.hpe.caf.worker.testing;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by ploch on 19/11/2015.
 */
public abstract class ContentFilesTestItemProvider implements TestItemProvider
{
    private final String inputPath;
    private final String expectedPath;
    private final String globPattern;
    private final boolean includeSubFolders;

    /**
     * Getter for property 'inputPath'.
     *
     * @return Value for property 'inputPath'.
     */
    public String getInputPath()
    {
        return inputPath;
    }

    /**
     * Getter for property 'expectedPath'.
     *
     * @return Value for property 'expectedPath'.
     */
    public String getExpectedPath()
    {
        return expectedPath;
    }

    public ContentFilesTestItemProvider(final String inputPath, final String expectedPath, final String globPattern, final boolean includeSubFolders)
    {

        this.inputPath = inputPath;
        this.expectedPath = expectedPath;
        this.globPattern = globPattern;
        this.includeSubFolders = includeSubFolders;
    }

    @Override
    public Collection<TestItem> getItems() throws Exception
    {

        List<Path> files = getFiles(Paths.get(inputPath));

        List<TestItem> testItems = new ArrayList<>(files.size());
        for (Path inputFile : files) {

            String fileName = inputFile.getFileName().toString();

            Path expectedFile = getExpectedFile(expectedPath, fileName);

            testItems.add(createTestItem(inputFile, expectedFile));
        }
        return testItems;
    }

    protected Path getExpectedFile(String expectedPath, String inputFileName)
    {
        Path expectedFile = Paths.get(expectedPath, inputFileName + ".result.xml");
        return expectedFile;
    }

    protected abstract TestItem createTestItem(Path inputFile, Path expectedFile) throws Exception;

    private List<Path> getFiles(Path directory) throws IOException
    {
        List<Path> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {

                if (Files.isDirectory(path)) {
                    if (includeSubFolders) {
                        fileNames.addAll(getFiles(path));
                    }
                } else if (globPattern == null || path.getFileSystem().getPathMatcher(globPattern).matches(path.getFileName())) {
                    fileNames.add(path);
                }
            }
        } catch (IOException ex) {
            System.out.println(ex);
            throw ex;
        }
        return fileNames;
    }
}
