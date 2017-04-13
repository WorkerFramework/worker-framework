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

import com.hpe.caf.api.CodecException;
import com.hpe.caf.worker.testing.TestExecutionException;
import com.hpe.caf.worker.testing.api.TestCaseInfo;
import com.hpe.caf.worker.testing.api.TestItem;
import com.hpe.caf.worker.testing.storage.TestItemDescriptor;
import com.hpe.caf.worker.testing.storage.TestItemRepository;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by ploch on 17/03/2017.
 */
public class ContentFilesGenerator implements DescriptorGenerator
{

    private final String inputFilesLocation;
    private final boolean recursive;
    private final String globPattern;
    private final TestCaseInfoFactory testCaseInfoFactory;
    private final TestInputFactory testInputFactory;
    private final TestItemRepository repository;

    public ContentFilesGenerator(String inputFilesLocation, boolean recursive, String globPattern, TestCaseInfoFactory testCaseInfoFactory, TestInputFactory testInputFactory, TestItemRepository repository)
    {

        this.inputFilesLocation = inputFilesLocation;
        this.recursive = recursive;
        this.globPattern = globPattern;
        this.testCaseInfoFactory = testCaseInfoFactory;
        this.testInputFactory = testInputFactory;
        this.repository = repository;
    }

    @Override
    public Collection<TestItemDescriptor> generate() throws IOException
    {
        Path path = Paths.get(inputFilesLocation);
        Collection<TestItemDescriptor> descriptors = new ArrayList<>();

        processPath(path, descriptors);
        return descriptors;
    }

    private void processPath(Path directory, Collection<TestItemDescriptor> descriptors) throws IOException
    {

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {

                if (Files.isDirectory(path)) {
                    if (recursive) {
                        //fileNames.addAll(getFiles(path));
                        processPath(path, descriptors);
                    }
                }
                else {

                    if (globPattern == null || path.getFileSystem().getPathMatcher(globPattern).matches(path.getFileName())) {
                        // fileNames.add(path);

                        TestItemDescriptor descriptor = createDescriptor(path);
                        descriptors.add(descriptor);

                        TestItem testItem = new TestItem(descriptor.getTestCaseInfo(), descriptor.getInputData(), null, path.getParent().toAbsolutePath().toString());
                        repository.saveDescriptor(testItem);
                    }
                }
            }
        }
        catch (IOException ex) {
            System.out.println(ex);
            throw ex;
        }
        catch (CodecException e) {
            throw new TestExecutionException("Generation failed", e);
        }
    }

    private TestItemDescriptor createDescriptor(Path path)
    {
        TestItemDescriptor descriptor = new TestItemDescriptor();
        TestCaseInfo info = testCaseInfoFactory.create(path);
        descriptor.setTestCaseInfo(info);

        Object testInput = testInputFactory.createTestInput(path);
        descriptor.setInputData(testInput);
        return descriptor;
    }
}
