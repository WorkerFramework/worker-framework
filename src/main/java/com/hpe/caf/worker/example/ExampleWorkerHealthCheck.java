package com.hpe.caf.worker.example;

import com.hpe.caf.api.HealthReporter;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Health check for example worker, displays health on the Marathon GUI
 */
public class ExampleWorkerHealthCheck implements HealthReporter {

    //for logging
    private static final Logger LOG = LoggerFactory.getLogger(ExampleWorker.class);

    public ExampleWorkerHealthCheck() {
    }

    /**
     * Health check makes sure all components that the worker depends on are available and returns a health result which
     * is displayed on Marathon GUI.
     * @return - HealthResult
     */
    @Override
    public HealthResult healthCheck() {
        try {
            //Using ModuleLoader, make sure the ExampleWorkerFactoryProvider implementation is available.
            ExampleWorkerFactoryProvider provider = ModuleLoader.getService(ExampleWorkerFactoryProvider.class);
            return HealthResult.RESULT_HEALTHY;
        } catch (ModuleLoaderException e) {
            //There was an issue with loading the factory provider implementation and an unhealthy HealthResult will be returned
            LOG.warn("Error loading module", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Module load failed. " + e.getMessage());
        }
    }
}
