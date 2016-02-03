package com.hpe.caf.worker.example;

import com.hpe.caf.worker.testing.execution.TestRunner;
import org.junit.Test;

/**
 * Integration test for Example worker, running the testing framework.
 */
public class ExampleWorkerAcceptanceIT {

    /**
     * Run integration tests using the testing framework.
     * @throws Exception
     */
    @Test
    public void testWorker() throws Exception {
        TestRunner.runTests(new ExampleTestControllerProvider());
    }
}
