/*
 * Copyright 2018-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.worker.testing.configuration;

/**
 * Created by ploch on 04/12/2015.
 */
public class DataStoreSettings
{
    private boolean useDataStore;

    private String dataStoreContainerId;

    public DataStoreSettings(boolean useDataStore, String dataStoreContainerId)
    {
        this.useDataStore = useDataStore;
        this.dataStoreContainerId = dataStoreContainerId;
    }

    public DataStoreSettings()
    {
    }

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
     * Getter for property 'dataStoreContainerId'.
     *
     * @return Value for property 'dataStoreContainerId'.
     */
    public String getDataStoreContainerId()
    {
        return dataStoreContainerId;
    }

    /**
     * Setter for property 'dataStoreContainerId'.
     *
     * @param dataStoreContainerId Value to set for property 'dataStoreContainerId'.
     */
    public void setDataStoreContainerId(String dataStoreContainerId)
    {
        this.dataStoreContainerId = dataStoreContainerId;
    }
}
