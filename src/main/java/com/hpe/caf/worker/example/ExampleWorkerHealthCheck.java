package com.hpe.caf.worker.example;

import com.hpe.caf.api.HealthReporter;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Created by smitcona on 22/01/2016.
 */
public class ExampleWorkerHealthCheck implements HealthReporter {

    //logger
    private static final Logger LOG = LoggerFactory.getLogger(ExampleWorker.class);

    public ExampleWorkerHealthCheck() {

    }

    /**
     * Health check makes sure all components that the worker depends on are available.
     * @return
     */
    @Override
    public HealthResult healthCheck() {
        try {
            //Using ModuleLoader, make sure the ExampleWorkerFactoryProvider implementation is available.
            ExampleWorkerFactoryProvider provider = ModuleLoader.getService(ExampleWorkerFactoryProvider.class);
//            WorkerFactory factory = provider.getWorkerFactory()
            return HealthResult.RESULT_HEALTHY;
        } catch (ModuleLoaderException e) {
            //There was an issue with loading the factory provider implementation and an unhealthy HealthResult will be returned
            LOG.warn("Error loading module", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Module load failed. " + e.getMessage());
        }
//        catch (WorkerException e){
//            LOG.warn("Worker exception", e);
//            return new HealthResult(HealthStatus.UNHEALTHY, "Worker exception. " + e.getMessage());
//        }
    }
}
