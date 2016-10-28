package com.hpe.caf.worker.example;

import com.hpe.caf.util.ref.ReferencedData;

public final class ExampleWorkerResultAccessors {
    private ExampleWorkerResultAccessors() {
    }

    public static ReferencedData getTextData(final ExampleWorkerResult exampleWorkerResult) {
        return exampleWorkerResult.textData;
    }
}
