package com.hpe.caf.worker.example;

import com.hpe.caf.worker.testing.execution.TestRunner;
import org.junit.Test;

/**
 * Created by smitcona on 22/01/2016.
 */
public class ExampleWorkerAcceptanceIT {

    @Test
    public void testWorker() throws Exception {
        TestRunner.runTests(new ExampleTestControllerProvider());
    }
}
