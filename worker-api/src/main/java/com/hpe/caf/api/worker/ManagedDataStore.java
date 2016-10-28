package com.hpe.caf.api.worker;


import com.hpe.caf.api.HealthReporter;


/**
 * A DataStore with management methods for use within an application.
 */
public interface ManagedDataStore extends HealthReporter, DataStore
{
    /**
     * @return metrics for the data store
     */
    DataStoreMetricsReporter getMetrics();


    /**
     * Perform necessary shut down operations.
     */
    void shutdown();
}
