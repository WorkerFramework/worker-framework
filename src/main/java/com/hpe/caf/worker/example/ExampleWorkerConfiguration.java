package com.hpe.caf.worker.example;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Created by smitcona on 22/01/2016.
 */
public class ExampleWorkerConfiguration {

    @NotNull
    @Size(min = 1)
    private String outputQueue;

    @Min(1)
    @Max(20)
    private int threads;

    /**
     * The size, in bytes, at which to write results to the DataStore instead of keeping them just in memory.
     */
    @Min(1024)
    @Max(100 * 1024)
    private int resultSizeThreshold;

    public ExampleWorkerConfiguration() { }

    public String getOutputQueue() {
        return outputQueue;
    }

    public void setOutputQueue(String outputQueue) {
        this.outputQueue = outputQueue;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getResultSizeThreshold() {
        return resultSizeThreshold;
    }

    public void setResultSizeThreshold(int resultSizeThreshold) {
        this.resultSizeThreshold = resultSizeThreshold;
    }
}
