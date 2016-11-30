/*
 * (c) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hpe.caf.worker.testing.configuration;

import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by ploch on 04/12/2015.
 */
public class ConfigurationBuilderTest {

    class TestIn{}

    class TestExpectation {}

    class TestTask {}

    class TestResult {}

    @Test
    public void testAllSettingsWithSpecifiedDocumentFolder() throws Exception {

        String containerId = UUID.randomUUID().toString();
        TestCaseSettings settings = ConfigurationBuilder
                .configure(TestTask.class, TestResult.class, TestIn.class, TestExpectation.class)
                .setUseDataStore(true)
                .setDataStoreContainerId(containerId)
                .setTestCaseFolder("test-data")
                .setDocumentFolder("test-data/documents")
                .build();

        assertThat(settings.getDataStoreSettings().getDataStoreContainerId(), equalTo(containerId));
        assertThat(settings.getDataStoreSettings().isUseDataStore(), equalTo(true));
        assertThat(settings.getTestDataSettings().getTestCaseFolder(), equalTo("test-data"));
        assertThat(settings.getTestDataSettings().getDocumentFolder(), equalTo("test-data/documents"));
        assertThat(settings.getWorkerClasses().getWorkerTaskClass(), equalTo(TestTask.class));
        assertThat(settings.getWorkerClasses().getWorkerResultClass(), equalTo(TestResult.class));
        assertThat(settings.getTestCaseClasses().getInputClass(), equalTo(TestIn.class));
        assertThat(settings.getTestCaseClasses().getExpectationClass(), equalTo(TestExpectation.class));

    }

}
