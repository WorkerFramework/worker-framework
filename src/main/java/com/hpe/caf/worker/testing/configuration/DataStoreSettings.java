package com.hpe.caf.worker.testing.configuration;

/**
 * Created by ploch on 04/12/2015.
 */
public class DataStoreSettings {

    private boolean useDataStore;

    private String dataStoreContainerId;

    public DataStoreSettings(boolean useDataStore, String dataStoreContainerId) {
        this.useDataStore = useDataStore;
        this.dataStoreContainerId = dataStoreContainerId;
    }

    public DataStoreSettings() {
    }

    /**
     * Getter for property 'useDataStore'.
     *
     * @return Value for property 'useDataStore'.
     */
    public boolean isUseDataStore() {
        return useDataStore;
    }

    /**
     * Setter for property 'useDataStore'.
     *
     * @param useDataStore Value to set for property 'useDataStore'.
     */
    public void setUseDataStore(boolean useDataStore) {
        this.useDataStore = useDataStore;
    }

    /**
     * Getter for property 'dataStoreContainerId'.
     *
     * @return Value for property 'dataStoreContainerId'.
     */
    public String getDataStoreContainerId() {
        return dataStoreContainerId;
    }

    /**
     * Setter for property 'dataStoreContainerId'.
     *
     * @param dataStoreContainerId Value to set for property 'dataStoreContainerId'.
     */
    public void setDataStoreContainerId(String dataStoreContainerId) {
        this.dataStoreContainerId = dataStoreContainerId;
    }
}
