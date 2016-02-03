package com.hpe.caf.worker.example;

import com.hpe.caf.util.ref.ReferencedData;

import javax.validation.constraints.NotNull;

/**
 * The task supplied to the worker. This is the main means of communication to the worker, providing the ReferencedData and
 * the action to take.
 */
public class ExampleWorkerTask {

    /**
     * The ReferencedData file in the DataStore.
     */
    @NotNull
    private ReferencedData sourceData;

    /**
     * Reference to the sourceData file in the DataStore, used in "wrapAsReferencedData" in the ExampleWorker class.
     */
    private String datastorePartialReference;

    /**
     * Enum to represent which action the worker will perform.
     */
    private ExampleWorkerAction action;

    public ExampleWorkerTask(){
        //empty constructor for serialisation.
    }

    public ReferencedData getSourceData() {
        return sourceData;
    }

    public void setSourceData(ReferencedData sourceData) {
        this.sourceData = sourceData;
    }

    public String getDatastorePartialReference() {
        return datastorePartialReference;
    }

    public void setDatastorePartialReference(String datastorePartialReference) {
        this.datastorePartialReference = datastorePartialReference;
    }

    public ExampleWorkerAction getAction() {
        return action;
    }

    public void setAction(ExampleWorkerAction action) {
        this.action = action;
    }
}
