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
package com.hpe.caf.worker.testing.experiment;

import com.hpe.caf.worker.testing.api.InputFileData;
import com.hpe.caf.worker.testing.api.TestCaseInfo;
import com.hpe.caf.worker.testing.preparation.DescriptorGenerator;
import com.hpe.caf.worker.testing.storage.TestItemDescriptor;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by ploch on 08/03/2017.
 */
public class BinaryHashWorkerDescriptorGenerator implements DescriptorGenerator {

    private final String location;
    private String globPattern = null;

    public BinaryHashWorkerDescriptorGenerator(String location) {

        this.location = location;
    }
    @Override
    public Collection<TestItemDescriptor> generate() throws IOException {
        Path path = Paths.get(location);

        List<Path> files = getFiles(path);
        ArrayList<TestItemDescriptor> descriptors = new ArrayList<>();
        for (Path file : files) {
            BinaryHashWorkerTestInput testInput = new BinaryHashWorkerTestInput();
            InputFileData fileData = new InputFileData();
            fileData.setFilePath(file.toString());
            testInput.setInputFileData(fileData);
            TestItemDescriptor descriptor = new TestItemDescriptor();
            descriptor.setInputData(testInput);

            TestCaseInfo info = new TestCaseInfo(file.toFile().getName(), null, "Test case for file " + file.toString(), null );

            descriptor.setTestCaseInfo(info);

            descriptors.add(descriptor);

        }
        return descriptors;
    }

    private List<Path> getFiles(Path directory) throws IOException {
        List<Path> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {

                    if (globPattern == null || path.getFileSystem().getPathMatcher(globPattern).matches(path.getFileName())) {
                        fileNames.add(path);
                    }
                }
            }
         catch (IOException ex) {
            System.out.println(ex);
            throw ex;
        }
        return fileNames;
    }
}
