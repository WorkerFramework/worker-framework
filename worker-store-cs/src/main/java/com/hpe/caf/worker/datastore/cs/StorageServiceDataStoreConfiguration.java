/*
 * (c) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hpe.caf.worker.datastore.cs;


import com.hpe.caf.api.Configuration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


@Configuration
public class StorageServiceDataStoreConfiguration
{
    @NotNull
    @Size(min = 1)
    private String serverName;

    @NotNull
    @Min(1)
    @Max(65535)
    private int port;

    /**
     * Optional authentication configuration settings.
     */
    private KeycloakAuthenticationConfiguration authenticationConfiguration;

    public StorageServiceDataStoreConfiguration()
    {
    }


    public String getServerName()
    {
        return this.serverName;
    }


    public void setServerName(String serverName)
    {
        this.serverName = serverName;
    }


    public int getPort()
    {
        return this.port;
    }


    public void setPort(int port)
    {
        this.port = port;
    }

    /**
     * Getter for property authenticationSettings
     * @return authentication settings. If null, authentication is disabled.
     */
    public KeycloakAuthenticationConfiguration getAuthenticationConfiguration() {
        return authenticationConfiguration;
    }

    public void setAuthenticationConfiguration(KeycloakAuthenticationConfiguration authenticationConfiguration) {
        this.authenticationConfiguration = authenticationConfiguration;
    }
}