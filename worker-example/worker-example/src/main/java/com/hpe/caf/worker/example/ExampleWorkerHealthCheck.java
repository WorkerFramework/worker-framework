/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.worker.example;

import com.hpe.caf.api.HealthReporter;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Health check for the example worker, health is displayed on the Marathon GUI.
 */
public class ExampleWorkerHealthCheck implements HealthReporter
{
    //for logging.
    private static final Logger LOG = LoggerFactory.getLogger(ExampleWorker.class);

    private static final String CAF_EXAMPLE_WORKER_HEALTHY = "CAF_EXAMPLE_WORKER_HEALTHY";

    /**
     * The health check checks if all the external components that the worker depends on are available. The health result is displayed on
     * Marathon GUI.
     *
     * @return - HealthResult
     */
    @Override
    public HealthResult healthCheck()
    {
        // Check that all worker components are available. If the worker depends on an external service, check that the service is accessible here.
        // In this scenario, the example worker does not depend on any external components. It will always return HealthResult.RESULT_HEALTHY.
        // If a service was not available, catch any exceptions, log a warning and return a UNHEALTHY health result.

        // If the environment variable CAF_EXAMPLE_WORKER_HEALTHY is set to false the healthCheck()
        // will return unhealthy otherwise it will always return healthy. This adds to the Example
        // Worker's testing and demonstration ability.
        HealthResult result = new HealthResult(HealthStatus.HEALTHY);
        String cafExampleWorkerHealth = System.getProperty(CAF_EXAMPLE_WORKER_HEALTHY, System.getenv(CAF_EXAMPLE_WORKER_HEALTHY));
        if (null != cafExampleWorkerHealth && cafExampleWorkerHealth.equalsIgnoreCase("false")) {
            LOG.debug("The Example Worker's Health Check is set to Unhealthy");
            result = new HealthResult(HealthStatus.UNHEALTHY);
        }
        return result;
    }

}
