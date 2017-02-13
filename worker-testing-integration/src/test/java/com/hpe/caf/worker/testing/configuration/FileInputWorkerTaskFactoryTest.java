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
package com.hpe.caf.worker.testing.configuration;

import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.testing.FileInputWorkerTaskFactory;
import com.hpe.caf.worker.testing.FileTestInputData;
import com.hpe.caf.worker.testing.TestItem;
import com.sun.management.UnixOperatingSystemMXBean;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.FileSystemException;
import java.util.UUID;

public class FileInputWorkerTaskFactoryTest {

    class TestFileInputWorkerTaskFactory extends FileInputWorkerTaskFactory {

        public TestFileInputWorkerTaskFactory(DataStore dataStore, String containerId, String testFilesFolder,
                                              String testSourcefileBaseFolder,
                                              String overrideReference) throws Exception {
            super(dataStore, containerId, testFilesFolder, testSourcefileBaseFolder, overrideReference);
        }

        @Override
        protected Object createTask(TestItem testItem, ReferencedData sourceData) {
            return null;
        }

        @Override
        public String getWorkerName() {
            return null;
        }

        @Override
        public int getApiVersion() {
            return 0;
        }
    }

    public FileInputWorkerTaskFactoryTest() throws Exception {
    }

    @Test
    public void testFileInputWorkerTaskFactoryCreateTaskDoesNotThrowFileSystemException () throws Exception {

        // Mock DataStore
        DataStore mockDataStore = Mockito.mock(DataStore.class);
        Mockito.when(mockDataStore.store(Mockito.any(InputStream.class), Mockito.any(String.class)))
                .thenReturn("mockRefId");

        String containerId = "mockContainerId";
        String testFilesFolder = "";
        String testSourcefileBaseFolder = "";
        TestFileInputWorkerTaskFactory testFileInputWorkerTaskFactory = new TestFileInputWorkerTaskFactory(
                mockDataStore, containerId, testFilesFolder, testSourcefileBaseFolder, null);

        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        int numberOfTimesToRun = 3000;
        if(os instanceof UnixOperatingSystemMXBean){
            numberOfTimesToRun = (int) ((UnixOperatingSystemMXBean) os).getMaxFileDescriptorCount() + 1;
            System.out.println("Number of open fd: " + ((UnixOperatingSystemMXBean) os).getMaxFileDescriptorCount());
        }

        for (int i = 0; i < numberOfTimesToRun; i++) {
            FileTestInputData fileTestInputData = new FileTestInputData();
            fileTestInputData.setInputFile("src\\test\\resources\\mockFile.txt");
            fileTestInputData.setUseDataStore(true);

            TestItem testItem = new TestItem("mockFile.txt", fileTestInputData, null);
            testItem.setInputIdentifier(UUID.randomUUID().toString());
            try {
                testFileInputWorkerTaskFactory.createTask(testItem);
            } catch (DataStoreException exception) {
                String exceptionStackTrace = ExceptionUtils.getStackTrace(exception);
                if (exception.getCause() instanceof FileSystemException) {
                    Assert.fail(String.format("Failed for FileSystemException cause: %s", exceptionStackTrace));
                }
                Assert.fail(String.format("Failed for: %s", exceptionStackTrace));
            }
        }
    }

}
