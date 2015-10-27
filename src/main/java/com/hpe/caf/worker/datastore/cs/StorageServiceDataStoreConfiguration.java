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

}