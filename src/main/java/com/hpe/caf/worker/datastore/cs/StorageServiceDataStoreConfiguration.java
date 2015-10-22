package com.hpe.caf.worker.datastore.cs;

import com.hpe.caf.api.Configuration;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 */
@Configuration
public class StorageServiceDataStoreConfiguration {

    @NotNull
    @Size(min = 1)
    private String serverName;

    @NotNull
    @Size(min = 1)
    private String port;

    @NotNull
    @Size(min = 1)
    private String emailAddress;

    public StorageServiceDataStoreConfiguration() {
    }

    public String getServerName() {
        return this.serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getPort() {
        return this.port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getEmailAddress() {
        return this.emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}