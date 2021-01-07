/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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

    /**
     * The data directory healthcheck timeout in seconds.
     */
    private Integer dataDirHealthcheckTimeoutSeconds;

    /**
     * The buffer size to use when storing data to the filesystem.
     * <p>
     * Note that not all storage methods currently respect this setting.
     */
    private Integer outputBufferSize;

    public String getDataDir()
    {
        return dataDir;
    }

    public void setDataDir(final String dataDir)
    {
        this.dataDir = dataDir;
    }

    public Integer getDataDirHealthcheckTimeoutSeconds()
    {
        return dataDirHealthcheckTimeoutSeconds;
    }

    public void setDataDirHealthcheckTimeoutSeconds(final Integer dataDirHealthcheckTimeoutSeconds)
    {
        this.dataDirHealthcheckTimeoutSeconds = dataDirHealthcheckTimeoutSeconds;
    }

    public Integer getOutputBufferSize()
    {
        return outputBufferSize;
    }

    public void setOutputBufferSize(final Integer outputBufferSize)
    {
        this.outputBufferSize = outputBufferSize;
    }
}
