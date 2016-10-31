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
