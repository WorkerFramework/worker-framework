package com.hpe.caf.worker.messagebuilder;

import com.hpe.caf.messagebuilder.Document;
import com.hpe.caf.messagebuilder.DocumentMessageBuilderException;
import com.hpe.caf.messagebuilder.DocumentServices;
import com.hpe.caf.messagebuilder.TaskMessage;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.example.ExampleWorkerAction;
import com.hpe.caf.worker.example.ExampleWorkerConstants;
import com.hpe.caf.worker.example.ExampleWorkerTask;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Testing the functionality of the ExampleWorkerTaskBuilder.
 */
public class ExampleWorkerTaskBuilderTest {
    @Test
    public void buildExampleWorkerTaskMessage() throws DocumentMessageBuilderException, IOException {
        //set up a document to be retrievable by builder
        Document testDoc = new TestDocumentImpl();
        String testStorageReference = UUID.randomUUID().toString();
        testDoc.setStorageReference(testStorageReference);
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
        Assert.assertNotNull("Message returned from builder should not be null", returnedMessage);

        //check the task returned has the expected API version and classifier
        Assert.assertEquals("Expecting correct task classifier on builder task message.",
                ExampleWorkerConstants.WORKER_NAME, returnedMessage.getTaskClassifier());
        Assert.assertEquals("Expecting correct task api version on builder task message.",
                ExampleWorkerConstants.WORKER_API_VER, returnedMessage.getTaskApiVersion());

        Object returnedDataAsObject = returnedMessage.getTaskData();
        ExampleWorkerTask returnedTaskdata = (ExampleWorkerTask) returnedDataAsObject;
        Assert.assertNotNull("Expecting returned task data from builder to not be null when deserialized.", returnedTaskdata);

        Assert.assertEquals("Expecting action on returned task data from builder to be that originally passed in.",
                testAction, returnedTaskdata.getAction());
        Assert.assertEquals("Expecting data source on returned task data from builder to be that originally passed in.",
                testDataStoreValue, returnedTaskdata.getDatastorePartialReference());
        ReferencedData returnedStorageReference = returnedTaskdata.getSourceData();
        Assert.assertEquals("Expecting storage reference returned from build to be that originally passed in.",
                testStorageReference, returnedStorageReference.getReference());
    }
}
