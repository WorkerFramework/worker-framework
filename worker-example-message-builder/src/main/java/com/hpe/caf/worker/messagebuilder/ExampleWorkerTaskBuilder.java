package com.hpe.caf.worker.messagebuilder;

import com.hpe.caf.messagebuilder.*;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.example.ExampleWorkerAction;
import com.hpe.caf.worker.example.ExampleWorkerConstants;
import com.hpe.caf.worker.example.ExampleWorkerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Builds a task to send to the Example Worker
 */
public class ExampleWorkerTaskBuilder implements DocumentMessageBuilder {
    private static final String dataStorePartialReferenceKey = "datastorePartialReference";
    private static final String actionKey = "action";

    public TaskMessage buildMessage(DocumentServices documentServices, Map<String, String> taskMessageParams) throws DocumentMessageBuilderException{
        try {
            Objects.requireNonNull(documentServices, "documentServices must not be null.");
            Objects.requireNonNull(taskMessageParams, "taskMessageParams must not be null.");
            Document document = documentServices.getDocument();
            Objects.requireNonNull(document, "Document provided by documentServices must not be null.");

            return buildExampleWorkerMessage(document, taskMessageParams);
        }
        catch(RuntimeException e){
            //catching all runtime exceptions as this specific type to make it easier for caller to defend against them
            throw new DocumentMessageBuilderException("Unable to build task message.", e);
        }
    }

    /**
     * Construct worker specific task. Relying on document and taskMessageParams having been validated as not null.
     * @param document
     * @param taskMessageParams
     * @return
     */
    private TaskMessage buildExampleWorkerMessage(Document document, Map<String, String> taskMessageParams){
        ExampleWorkerTask taskData = new ExampleWorkerTask();
        String storageReference = document.getStorageReference();
        Objects.requireNonNull(storageReference, "storageReference on document must not be null.");
        taskData.sourceData = ReferencedData.getReferencedData(document.getStorageReference());

        if(!taskMessageParams.containsKey(dataStorePartialReferenceKey)){
            throw new NullPointerException("'datastorePartialReference' must be provided in 'taskMessageParams'.");
        }
        String datastorePartialReference = taskMessageParams.get(dataStorePartialReferenceKey);
        Objects.requireNonNull(datastorePartialReference, "datastorePartialReference' on taskMessageParams must not be null.");

        taskData.datastorePartialReference = datastorePartialReference;

        String actionStr = taskMessageParams.get(actionKey);
        ExampleWorkerAction actionToSet;
        try
        {
            actionToSet = ExampleWorkerAction.valueOf(actionStr);
        }
        catch(IllegalArgumentException e){
            throw new IllegalArgumentException("Unable to derive ExampleWorkerAction value from value on taskMessageParam. key " + actionKey +
                                 ", value: "+actionStr, e);
        }
        taskData.action = actionToSet;
        return constructTaskMessageFromExampleWorkerTask(taskData);
    }

    private TaskMessage constructTaskMessageFromExampleWorkerTask(ExampleWorkerTask exampleWorkerTask){
        TaskMessage taskMessage = new TaskMessage();
        taskMessage.setTaskData(exampleWorkerTask);
        taskMessage.setTaskApiVersion(ExampleWorkerConstants.WORKER_API_VER);
        taskMessage.setTaskClassifier(ExampleWorkerConstants.WORKER_NAME);
        return taskMessage;
    }
}
