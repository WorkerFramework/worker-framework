/*
 * Copyright 2018-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.worker.messagebuilder;

import com.hpe.caf.messagebuilder.Document;
import com.hpe.caf.messagebuilder.DocumentMessageBuilderException;
import com.hpe.caf.messagebuilder.DocumentServices;
import com.hpe.caf.messagebuilder.TaskMessage;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.example.ExampleWorkerAction;
import com.hpe.caf.worker.example.ExampleWorkerConstants;
import com.hpe.caf.worker.example.ExampleWorkerTask;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Testing the functionality of the ExampleWorkerTaskBuilder.
 */
public class ExampleWorkerTaskBuilderTest
{

    @Test
    public void buildExampleWorkerTaskMessage() throws DocumentMessageBuilderException, IOException
    {
        //set up a document to be retrievable by builder
        String testStorageReference = UUID.randomUUID().toString();
        Document testDoc = new TestDocumentImpl(testStorageReference);
        DocumentServices testDocServices = () -> testDoc;

        //set up taskMessageParams to construct the TaskMessage from in the builder
        Map<String, String> taskMessageParams = new HashMap<>();
        String dataStoreKey = "datastorePartialReference";
        String testDataStoreValue = "88/1234";
        String actionKey = "action";
        ExampleWorkerAction testAction = ExampleWorkerAction.CAPITALISE;
        taskMessageParams.put(dataStoreKey, testDataStoreValue);
        taskMessageParams.put(actionKey, testAction.toString());

        ExampleWorkerTaskBuilder builder = new ExampleWorkerTaskBuilder();
        TaskMessage returnedMessage = builder.buildMessage(testDocServices, taskMessageParams);
        Assert.assertNotNull(returnedMessage, "Message returned from builder should not be null");

        //check the task returned has the expected API version and classifier
        Assert.assertEquals(ExampleWorkerConstants.WORKER_NAME, returnedMessage.getTaskClassifier(),
                            "Expecting correct task classifier on builder task message.");
        Assert.assertEquals(ExampleWorkerConstants.WORKER_API_VER, returnedMessage.getTaskApiVersion(),
                            "Expecting correct task api version on builder task message.");

        Object returnedDataAsObject = returnedMessage.getTaskData();
        ExampleWorkerTask returnedTaskdata = (ExampleWorkerTask) returnedDataAsObject;
        Assert.assertNotNull(returnedTaskdata, "Expecting returned task data from builder to not be null when deserialized.");

        Assert.assertEquals(testAction, returnedTaskdata.action,
                            "Expecting action on returned task data from builder to be that originally passed in.");
        Assert.assertEquals(testDataStoreValue, returnedTaskdata.datastorePartialReference,
                            "Expecting data source on returned task data from builder to be that originally passed in.");
        ReferencedData returnedStorageReference = returnedTaskdata.sourceData;
        Assert.assertEquals(testStorageReference, returnedStorageReference.getReference(),
                            "Expecting storage reference returned from build to be that originally passed in.");
    }
}
