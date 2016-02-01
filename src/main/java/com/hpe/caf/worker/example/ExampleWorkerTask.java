package com.hpe.caf.worker.example;

import com.hpe.caf.util.ref.ReferencedData;

import javax.validation.constraints.NotNull;

/**
 * Worker task supplied to the worker as the main input communication.
 */
public class ExampleWorkerTask {
    /**
     * The ReferencedData file in datastore of the source data.
     */
    @NotNull
    private ReferencedData sourceData;

    /**
     * Reference to the sourceData in the datastore, used to store data in the datastore.
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
