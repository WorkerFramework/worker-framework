package com.hpe.caf.worker.datastore.fs;


import com.hpe.caf.api.Configuration;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


@Configuration
public class FileSystemDataStoreConfiguration
{
    /**
     * The directory to store and retrieve data from on the disk.
     */
    @NotNull
    @Size(min = 1)
    private String dataDir = "datastore";


    public String getDataDir()
    {
        return dataDir;
    }


    public void setDataDir(final String dataDir)
    {
        this.dataDir = dataDir;
    }
}
