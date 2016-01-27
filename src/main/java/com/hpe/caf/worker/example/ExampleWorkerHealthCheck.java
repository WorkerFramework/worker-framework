package com.hpe.caf.worker.example;

import com.hpe.caf.api.HealthReporter;
import com.hpe.caf.api.HealthResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Created by smitcona on 22/01/2016.
 */
public class ExampleWorkerHealthCheck implements HealthReporter {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleWorker.class);

    public ExampleWorkerHealthCheck() {

    }

    @Override
    public HealthResult healthCheck() {
//        try {
//            ExampleWorkerFactoryProvider provider = ModuleLoader.getService(ExampleWorkerFactoryProvider.class);
            return HealthResult.RESULT_HEALTHY;
//        }
//        catch (ModuleLoaderException e) {
//            LOG.warn("Error loading module", e);
//            return new HealthResult(HealthStatus.UNHEALTHY, "Module load failed. " + e.getMessage());
//        }
//        } catch (WorkerException e){
//            LOG.warn("Worker exception", e);
//            return new HealthResult(HealthStatus.UNHEALTHY, "Worker exception. " + e.getMessage());
//        }
    }
}
