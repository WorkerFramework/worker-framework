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
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreSource;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.util.ref.DataSource;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.example.ExampleWorker;
import com.hpe.caf.worker.example.ExampleWorkerAction;
import com.hpe.caf.worker.example.ExampleWorkerResult;
import com.hpe.caf.worker.example.ExampleWorkerTask;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * Unit test to verify the worker correctly performs the desired action.
 */
public class ExampleWorkerTest {

    /**
     * Unit test for testing the worker's reverse action.
     * Create a referenced data object,
     * Create a worker task using the referenced data object,
     * Create a worker using the factory provider,
     * Compare the result to the expected result.
     */
    @Test
    public void testExampleWorkerReverse() throws Exception {
        //Codec
        Codec codec = new JsonCodec();

        //Mock DataStore
        DataStore mockDataStore = Mockito.mock(DataStore.class);
        Mockito.when(mockDataStore.store(Mockito.any(InputStream.class), Mockito.any(String.class)))
                .thenReturn("mockRefId");
        DataSource mockSource = new DataStoreSource(mockDataStore, codec);

        //Create the worker subject to testing
        ExampleWorker worker = new ExampleWorker(createMockTask(ExampleWorkerAction.REVERSE), mockDataStore, "mockQueue", codec, 1024);

        //Test
        WorkerResponse response = worker.doWork();

        //verify results
        Assert.assertEquals(TaskStatus.RESULT_SUCCESS, response.getTaskStatus());
        ExampleWorkerResult workerResult = codec.deserialise(response.getData(), ExampleWorkerResult.class);
        Assert.assertNotNull(workerResult);
        ReferencedData resultRefData = workerResult.textData;
        String resultText = streamToString(resultRefData.acquire(mockSource), "UTF-8");
        Assert.assertTrue(resultText.startsWith("etats fo yraterceS"));
    }

    /**
     * Unit test for testing the worker's capitalise action.
     * Create a referenced data object,
     * Create a worker task using the referenced data object,
     * Create a worker using the factory provider,
     * Compare the result to the expected result.
     */
    @Test
    public void testExampleWorkerCapitalise() throws Exception {
        //Codec
        Codec codec = new JsonCodec();

        //Mock DataStore
        DataStore mockDataStore = Mockito.mock(DataStore.class);
        Mockito.when(mockDataStore.store(Mockito.any(InputStream.class), Mockito.any(String.class)))
                .thenReturn("mockRefId");
        DataSource mockSource = new DataStoreSource(mockDataStore, codec);

        //Create the worker subject to testing
        ExampleWorker worker = new ExampleWorker(createMockTask(ExampleWorkerAction.CAPITALISE), mockDataStore, "mockQueue", codec, 1024);

        //Test
        WorkerResponse response = worker.doWork();

        //verify results
        Assert.assertEquals(TaskStatus.RESULT_SUCCESS, response.getTaskStatus());
        ExampleWorkerResult workerResult = codec.deserialise(response.getData(), ExampleWorkerResult.class);
        Assert.assertNotNull(workerResult);
        ReferencedData resultRefData = workerResult.textData;
        String resultText = streamToString(resultRefData.acquire(mockSource), "UTF-8");
        Assert.assertTrue(resultText.startsWith("SECRETARY OF STATE"));
    }

    /**
     * Private method to create a mock task using a mocked referenced data object.
     * @param action
     * @return ExampleWorkerTask
     * @throws IOException
     */
    private ExampleWorkerTask createMockTask(ExampleWorkerAction action) throws IOException {
        String text = "Secretary of state";
        ReferencedData mockReferencedData = ReferencedData.getWrappedData(text.getBytes());
        ExampleWorkerTask task = new ExampleWorkerTask();
        task.sourceData = mockReferencedData;
        task.action = action;
        return task;
    }

    /**
     * Private method to convert the InputStream acquired from the DataStore to a string.
     * @param stream
     * @param encoding
     * @return String
     * @throws IOException
     */
    private String streamToString(InputStream stream, String encoding) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(stream, writer, encoding);
        return writer.toString();
    }
}
