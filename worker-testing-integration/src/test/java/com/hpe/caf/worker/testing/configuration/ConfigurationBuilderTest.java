/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.worker.testing.configuration;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.UUID;

public class ConfigurationBuilderTest
{
    class TestIn
    {
    }

    class TestExpectation
    {
    }

    class TestTask
    {
    }

    class TestResult
    {
    }

    @Test
    public void testAllSettingsWithSpecifiedDocumentFolder() throws Exception
    {
        String containerId = UUID.randomUUID().toString();
        TestCaseSettings settings = ConfigurationBuilder
            .configure(TestTask.class, TestResult.class, TestIn.class, TestExpectation.class)
            .setUseDataStore(true)
            .setDataStoreContainerId(containerId)
            .setTestCaseFolder("test-data")
            .setDocumentFolder("test-data/documents")
            .build();

        Assert.assertEquals(settings.getDataStoreSettings().getDataStoreContainerId(), containerId);
        Assert.assertEquals(settings.getDataStoreSettings().isUseDataStore(), true);
        Assert.assertEquals(settings.getTestDataSettings().getTestCaseFolder(), "test-data");
        Assert.assertEquals(settings.getTestDataSettings().getDocumentFolder(), "test-data/documents");
        Assert.assertEquals(settings.getWorkerClasses().getWorkerTaskClass(), TestTask.class);
        Assert.assertEquals(settings.getWorkerClasses().getWorkerResultClass(), TestResult.class);
        Assert.assertEquals(settings.getTestCaseClasses().getInputClass(), TestIn.class);
        Assert.assertEquals(settings.getTestCaseClasses().getExpectationClass(), TestExpectation.class);
    }
}
