package com.hpe.caf.worker.example;

import com.hpe.caf.util.ref.ReferencedData;

import javax.validation.constraints.NotNull;

/**
 * Created by smitcona on 22/01/2016.
 */
public class ExampleWorkerTask {
    /**
     * The ReferencedData file in datastore of the source data.
     */
    @NotNull
    private ReferencedData sourceData;


    private String datastorePartialReference;

    /**
     * String to represent which action the worker will perform.
     */
    private String action;

    /**
     * Empty constructor for serialisation purposes.
     */
    public ExampleWorkerTask(){

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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
