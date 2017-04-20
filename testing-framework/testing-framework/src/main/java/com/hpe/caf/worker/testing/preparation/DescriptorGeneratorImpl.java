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

import com.hpe.caf.worker.testing.api.TestDataException;
import com.hpe.caf.worker.testing.api.TestDataSource;
import com.hpe.caf.worker.testing.storage.TestItemDescriptor;
import com.hpe.caf.worker.testing.storage.TestItemRepository;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by ploch on 20/04/2017.
 */
public class DescriptorGeneratorImpl implements DescriptorGenerator
{
    private final Iterable<TestDataSource> testDataSourceProvider;
    private final TestItemDescriptorFactory descriptorFactory;
    private final TestItemRepository repository;

    public DescriptorGeneratorImpl(Iterable<TestDataSource> testDataSourceProvider, TestItemDescriptorFactory descriptorFactory, TestItemRepository repository)
    {
        this.testDataSourceProvider = testDataSourceProvider;
        this.descriptorFactory = descriptorFactory;
        this.repository = repository;
    }
    @Override
    public void generate() throws IOException, TestDataException
    {
        for (TestDataSource dataSource : testDataSourceProvider) {
            TestItemDescriptor descriptor = descriptorFactory.create(dataSource);

            String location = null;
            Path contentDirectory = dataSource.getData(Path.class, TestDataSourceIds.CONTENT_DIRECTORY);
            if (contentDirectory != null) {
                location = contentDirectory.toAbsolutePath().toString();
            }
            else {
                Path file = dataSource.getData(Path.class, TestDataSourceIds.CONTENT_FILE);
                if (file != null) {
                    location = file.getParent().toAbsolutePath().toString();
                }
            }
            repository.saveDescriptor(descriptor, location);
            /*TestItem testItem = new TestItem(descriptor.getTestCaseInfo(), descriptor.getInputData(), null, path.getParent().toAbsolutePath().toString());
            repository.saveDescriptor(testItem);*/
        }
    }
}
