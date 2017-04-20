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
package com.hpe.caf.worker.testing.extract;

import com.hpe.caf.worker.document.DocumentWorkerConstants;
import com.hpe.caf.worker.document.DocumentWorkerResult;
import com.hpe.caf.worker.document.DocumentWorkerTask;
import com.hpe.caf.worker.testing.TestBuilder;
import com.hpe.caf.worker.testing.TestController;
import com.hpe.caf.worker.testing.api.TestItem;
import com.hpe.caf.worker.testing.api.TestResult;
import com.hpe.caf.worker.testing.api.WorkerInfo;
import com.hpe.caf.worker.testing.api.WorkerTaskFactory;
import com.hpe.caf.worker.testing.storage.FileTestItemRepository;
import com.hpe.caf.worker.testing.storage.YamlTestCaseSerializer;
import com.hpe.caf.worker.testing.util.TestFilesUtil;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Created by ploch on 20/04/2017.
 */
public class DocWorkerIT
{
    @Test
    public void testDocExtractTest() throws Exception
    {
        WorkerInfo workerInfo = new WorkerInfo(DocumentWorkerConstants.WORKER_API_VER, DocumentWorkerConstants.WORKER_NAME, DocumentWorkerTask.class, DocumentWorkerResult.class);

        TestBuilder builder = new TestBuilder();
        //BinaryHashWorkerTaskFactory factory = new BinaryHashWorkerTaskFactory(builder.getWorkerServices().getDataStore());

        WorkerTaskFactory factory = new DocumentWorkerTaskFactory(builder.getWorkerServices().getDataStore());

        TestController controller = builder.createDefault(workerInfo, factory);
//        /*String root = Paths.get(TestFilesUtil.getTestDataRootPath(), "content-files", "in-folders").toAbsolutePath().toString();*/
        Path dataPath = TestFilesUtil.getTestDataPath("extract");
        FileTestItemRepository repository = new FileTestItemRepository(dataPath.toString(), new YamlTestCaseSerializer());
        Collection<TestItem> testItems = repository.retrieveTestItems();

        for (TestItem testItem : testItems) {
            TestResult testResult = controller.executeTest(testItem);
        }

        /*TestItem testItem = createTestItem();

        TestResult testResult = controller.executeTest(testItem);
*/
       // System.out.println(testResult.isSuccess());
    }
}
