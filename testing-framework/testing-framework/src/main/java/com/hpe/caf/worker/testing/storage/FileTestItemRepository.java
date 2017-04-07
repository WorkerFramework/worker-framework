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
package com.hpe.caf.worker.testing.storage;

import com.hpe.caf.worker.testing.TestExecutionException;
import com.hpe.caf.worker.testing.api.TestItem;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by ploch on 08/03/2017.
 */
public class FileTestItemRepository implements TestItemRepository {

    private final String repositoryPath;
    private final TestCaseSerializer serializer;

    public FileTestItemRepository(String repositoryPath, TestCaseSerializer serializer) {

        this.repositoryPath = repositoryPath;
        this.serializer = serializer;
    }

    public Collection<TestItem> retrieveTestItems() throws IOException {
        Stream<Path> pathStream = Files.find(Paths.get(repositoryPath), 3, new BiPredicate<Path, BasicFileAttributes>() {
            @Override
            public boolean test(Path path, BasicFileAttributes basicFileAttributes) {
                if (Files.isDirectory(path)) return false;
                return path.getFileName().toString().endsWith(".test.descriptor");
            }
        });

        List<TestItem> testItems = pathStream.map(path -> createFromDescriptorFile(path)).collect(Collectors.toList());

        return testItems;

    }

    private TestItem createFromDescriptorFile(Path descriptorPath) {
        TestItemDescriptor descriptor = deserializeDescriptor(descriptorPath);

        return new TestItem(descriptor.getTestCaseInfo(), descriptor.getInputData(), null, descriptorPath.toAbsolutePath().toString());

    }

    private TestItemDescriptor deserializeDescriptor(Path path) {
        try {
            return serializer.deserialise(Files.readAllBytes(path), TestItemDescriptor.class);
        } catch (IOException e) {
            throw new TestExecutionException("Failed to deserialize descriptor.", e);
        }
    }

    @Override
    public void saveDescriptor(TestItem testItem) throws IOException {
        TestItemDescriptor descriptor = new TestItemDescriptor();
        descriptor.setTestCaseInfo(testItem.getTestCaseInformation());
        descriptor.setInputData(testItem.getInputData());
        saveToFile(testItem.getLocation(), testItem.getTestCaseInformation().getTestCaseId(), "descriptor", descriptor);

       /* byte[] descriptorBytes = serializer.serialise(descriptor);
        String descriptorFileName = testItem.getTestCaseInformation().getTestCaseId() + ".test.descriptor";

        FileUtils.writeByteArrayToFile(new File(repositoryPath, descriptorFileName), descriptorBytes);*/

    }

    @Override
    public void saveExpectation(TestItem testItem) throws IOException {
        TestItemExpectation expectation = new TestItemExpectation();
        expectation.setCreated(new Date());
        expectation.setExpectation(testItem.getExpectedOutputData());

        saveToFile(testItem.getLocation(), testItem.getTestCaseInformation().getTestCaseId(), "expectation", expectation);
        /*byte[] expectationBytes = serializer.serialise(expectation);
        String expectationFileName = testItem.getTestCaseInformation().getTestCaseId() + ".test.expectation";

        FileUtils.writeByteArrayToFile(new File(repositoryPath, expectationFileName), expectationBytes);*/
    }

    private void saveToFile(String testItemLocation, String testId, String extensionSuffix, Object obj) throws IOException {
        byte[] bytes = serializer.serialise(obj);
        String fileName = String.format("%s.test.%s", testId, extensionSuffix);
        String location = testItemLocation == null ? repositoryPath : testItemLocation;
        FileUtils.writeByteArrayToFile(new File(location, fileName), bytes);
    }
}
