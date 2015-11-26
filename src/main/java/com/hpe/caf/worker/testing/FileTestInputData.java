package com.hpe.caf.worker.testing;

/**
 * Created by ploch on 19/11/2015.
 */
public class FileTestInputData {

    private boolean useDataStore;
    private String containerId;

    private String inputFile;

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
     * Getter for property 'containerId'.
     *
     * @return Value for property 'containerId'.
     */
    public String getContainerId() {
        return containerId;
    }

    /**
     * Setter for property 'containerId'.
     *
     * @param containerId Value to set for property 'containerId'.
     */
    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    /**
     * Getter for property 'inputFile'.
     *
     * @return Value for property 'inputFile'.
     */
    public String getInputFile() {
        return inputFile;
    }

    /**
     * Setter for property 'inputFile'.
     *
     * @param inputFile Value to set for property 'inputFile'.
     */
    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

}
