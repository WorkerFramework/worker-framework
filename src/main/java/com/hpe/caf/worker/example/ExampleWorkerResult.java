package com.hpe.caf.worker.example;

import com.hpe.caf.util.ref.ReferencedData;

import javax.validation.constraints.NotNull;

/**
 * Created by smitcona on 22/01/2016.
 */
public class ExampleWorkerResult {
    /**
     * Worker specific return code
     */
    @NotNull
    private ExampleWorkerStatus workerStatus;

    /**
     * Result file is stored in datastore and accessed using this ReferencedData object
     */
    private ReferencedData textData;


    public ExampleWorkerResult() {
    }

    public ExampleWorkerResult(ExampleWorkerStatus status){
        this.workerStatus = status;
    }

    public ExampleWorkerStatus getWorkerStatus() {
        return workerStatus;
    }

    public void setWorkerStatus(ExampleWorkerStatus workerStatus) {
        this.workerStatus = workerStatus;
    }

    public ReferencedData getTextData() {
        return textData;
    }

    public void setTextData(ReferencedData textData) {
        this.textData = textData;
    }
}