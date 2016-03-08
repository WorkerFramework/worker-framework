#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerFactoryProvider;

/**
 * ${workerName} factory provider implementation.
 */
public class ${workerName}FactoryProvider implements WorkerFactoryProvider {

    /**
     * Get the worker factory implementation.
     * Called by the ModuleLoader. ${workerName}FactoryProvider must be registered by the service file in resources/META-INF/services.
     * @param configSource
     * @param dataStore
     * @param codec
     * @return ${workerName}Factory
     * @throws WorkerException
     */
    @Override
    public WorkerFactory getWorkerFactory(final ConfigurationSource configSource, final DataStore dataStore, final Codec codec) throws WorkerException {
        return new ${workerName}Factory(configSource, dataStore, codec);
    }
}
