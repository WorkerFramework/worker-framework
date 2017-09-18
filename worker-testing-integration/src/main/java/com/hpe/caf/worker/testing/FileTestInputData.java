/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.worker.testing;

/**
 * Created by ploch on 19/11/2015.
 */
public class FileTestInputData
{
    private boolean useDataStore;
    private String containerId;

    private String inputFile;

    private String storageReference;

    /**
     * Getter for property 'useDataStore'.
     *
     * @return Value for property 'useDataStore'.
     */
    public boolean isUseDataStore()
    {
        return useDataStore;
    }

    /**
     * Setter for property 'useDataStore'.
     *
     * @param useDataStore Value to set for property 'useDataStore'.
     */
    public void setUseDataStore(boolean useDataStore)
    {
        this.useDataStore = useDataStore;
    }

    /**
     * Getter for property 'containerId'.
     *
     * @return Value for property 'containerId'.
     */
    public String getContainerId()
    {
        return containerId;
    }

    /**
     * Setter for property 'containerId'.
     *
     * @param containerId Value to set for property 'containerId'.
     */
    public void setContainerId(String containerId)
    {
        this.containerId = containerId;
    }

    /**
     * Getter for property 'inputFile'.
     *
     * @return Value for property 'inputFile'.
     */
    public String getInputFile()
    {
        return inputFile;
    }

    /**
     * Setter for property 'inputFile'.
     *
     * @param inputFile Value to set for property 'inputFile'.
     */
    public void setInputFile(String inputFile)
    {
        this.inputFile = inputFile;
    }

    /**
     * Getter for property 'storageReference'.
     *
     * @return Value for property 'storageReference'.
     */
    public String getStorageReference()
    {
        return storageReference;
    }

    /**
     * Setter for property 'storageReference'.
     *
     * @param storageReference Value to set for property 'storageReference'.
     */
    public void setStorageReference(String storageReference)
    {
        this.storageReference = storageReference;
    }
}
