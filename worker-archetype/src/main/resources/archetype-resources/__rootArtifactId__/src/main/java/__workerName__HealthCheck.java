#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.hpe.caf.api.HealthReporter;
import com.hpe.caf.api.HealthResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Health check for the ${workerName}, health is displayed on the Marathon GUI.
 */
public class ${workerName}HealthCheck implements HealthReporter {

    //for logging.
    private static final Logger LOG = LoggerFactory.getLogger(${workerName}.class);

    public ${workerName}HealthCheck() {
    }

    /**
     * The health check checks if all the external components that the worker depends on are available.
     * The health result is displayed on Marathon GUI.
     * @return - HealthResult
     */
    @Override
    public HealthResult healthCheck() {
        // Check that all worker components are available. If the worker depends on an external service, check that the service is accessible here.
        // In this scenario, the ${workerName} does not depend on any external components. It will always return HealthResult.RESULT_HEALTHY.
        // If a service was not available, catch any exceptions, log a warning and return a UNHEALTHY health result.

        return HealthResult.RESULT_HEALTHY;
    }
}
