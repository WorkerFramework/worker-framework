package com.hpe.caf.api.worker;


/**
 * Provides metrics for a DataStore.
 */
public interface DataStoreMetricsReporter
{
    /**
     * @return the number of 'store' requests so far
     */
    int getStoreRequests();


    /**
     * @return the number of 'retrieve' requests so far
     */
    int getRetrieveRequests();


    /**
     * @return the number of failures/errors encountered by the DataStore so far
     */
    int getErrors();
}
