package com.hpe.caf.worker.example;

import com.hpe.caf.util.ref.ReferencedData;

import javax.validation.constraints.NotNull;

/**
 * The result class of the worker, containing a worker status and a ReferencedData object for textData.
 */
public final class ExampleWorkerResult {

    /**
     * Worker specific return code.
     */
    @NotNull
    public ExampleWorkerStatus workerStatus;

    /**
     * Result file is stored in datastore and accessed using this ReferencedData object.
     */
    public ReferencedData textData;
}
