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

import com.hpe.caf.api.Encrypted;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class KeycloakAuthenticationConfiguration {

    /**
     * The Keycloak server host name
     */
    @NotNull
    @Size(min = 1)
    private String serverName;

    /**
     * The Keycloak server port
     */
    @NotNull
    @Min(1)
    @Max(65535)
    private int port;

    /**
     * The user name used when requesting access token
     */
    @NotNull
    @Size(min = 1)
    private String userName;

    /**
     * The user password used when requesting access token
     */
    @Encrypted
    private String password;

    /**
     * The client name used when requesting access token
     */
    @NotNull
    @Size(min = 1)
    private String clientName;

    /**
     * The client secret used when requesting access token
     */
    @Encrypted
    private String clientSecret;

    /**
     * The realm used when requesting access token
     */
    @NotNull
    @Size(min = 1)
    private String realm;

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * Getter for property 'port'.
     *
     * @return the value of Keycloak server port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Setter for property 'port'.
     *
     * @param port Value to set for property 'port'.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Getter for property 'userName'.
     *
     * @return Value for property 'userName'.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Setter for property 'userName'.
     *
     * @param userName Value to set for property 'userName'.
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Getter for property 'password'.
     *
     * @return Value for property 'password'.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Setter for property 'password'.
     *
     * @param password Value to set for property 'password'.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Getter for property 'clientName'.
     *
     * @return Value for property 'clientName'.
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * Setter for property 'clientName'.
     *
     * @param clientName Value to set for property 'clientName'.
     */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    /**
     * Getter for property 'clientSecret'.
     *
     * @return Value for property 'clientSecret'.
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Setter for property 'clientSecret'.
     *
     * @param clientSecret Value to set for property 'clientSecret'.
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * Getter for property 'realm'.
     *
     * @return Value for property 'realm'.
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Setter for property 'realm'.
     *
     * @param realm Value to set for property 'realm'.
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }
}
