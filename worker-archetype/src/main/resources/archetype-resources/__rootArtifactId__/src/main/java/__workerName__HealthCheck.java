/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
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
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.hpe.caf.api.HealthReporter;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Health check for the ${workerName}, health is displayed on the Marathon GUI.
 */
public class ${workerName}HealthCheck implements HealthReporter {

    //for logging.
    private static final Logger LOG = LoggerFactory.getLogger(${workerName}.class);

    private static final String CAF_EXAMPLE_WORKER_HEALTHY = "CAF_EXAMPLE_WORKER_HEALTHY";

    public ${workerName}HealthCheck() {
    }

    /**
     * The health check checks if all the external components that the worker depends on are available.
     * The health result is displayed on Marathon GUI.
     * @return - HealthResult
     */
    @Override
    public HealthResult healthCheck() {
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
