package com.hpe.caf.worker.example;

import com.hpe.caf.util.ref.ReferencedData;

import javax.validation.constraints.NotNull;

/**
 * The task supplied to the worker. This is the main means of communication to the worker, providing the ReferencedData and
 * the action to take.
 */
public final class ExampleWorkerTask {

    /**
     * The ReferencedData file in the DataStore.
     */
    @NotNull
    private ReferencedData sourceData;

    /**
     * Identifies a target (relative) location for any output data that worker will save in data store.
     * If datastore-cs is used, this value will identify a target data store container to use.
     * If a worker needs to store output data, this should be used when calling {@link com.hpe.caf.api.worker.DataStore#store(Path, String)} method.
     * Example usage is located in ExampleWorker class, 'wrapAsReferencedData' method.
     */
    private String datastorePartialReference;

    /**
     * Enumeration to represent which action the worker will perform.
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
