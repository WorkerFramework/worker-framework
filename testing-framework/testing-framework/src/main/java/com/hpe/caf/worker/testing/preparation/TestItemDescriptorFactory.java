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

import com.hpe.caf.worker.testing.api.TestCaseInfo;
import com.hpe.caf.worker.testing.api.TestDataException;
import com.hpe.caf.worker.testing.api.TestDataSource;
import com.hpe.caf.worker.testing.storage.TestItemDescriptor;

/**
 * Created by ploch on 20/04/2017.
 */
public class TestItemDescriptorFactory
{

    private final TestCaseInfoFactory testCaseInfoFactory;
    private final TestInputFactory testInputFactory;

    public TestItemDescriptorFactory(TestCaseInfoFactory testCaseInfoFactory, TestInputFactory testInputFactory)
    {
        this.testCaseInfoFactory = testCaseInfoFactory;
        this.testInputFactory = testInputFactory;
    }

    public TestItemDescriptor create(TestDataSource dataSource) throws TestDataException
    {
        TestItemDescriptor descriptor = new TestItemDescriptor();
        TestCaseInfo info = testCaseInfoFactory.create(dataSource);
        descriptor.setTestCaseInfo(info);

        Object testInput = testInputFactory.createTestInput(dataSource);
        descriptor.setInputData(testInput);
        return descriptor;
    }
}
