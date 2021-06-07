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
package com.hpe.caf.worker.datastore.http;

import com.hpe.caf.api.Configuration;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Configuration
public class HttpDataStoreConfiguration
{
    @NotNull
    @Size(min = 1)
    private String url;

    private Integer connectTimeoutMillis;

    private Integer readTimeoutMillis;

    public String getUrl()
    {
        return url;
    }

    public void setUrl(final String url)
    {
        this.url = url;
    }

    public Integer getConnectTimeoutMillis()
    {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(final Integer connectTimeoutMillis)
    {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public Integer getReadTimeoutMillis()
    {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(final int readTimeoutMillis)
    {
        this.readTimeoutMillis = readTimeoutMillis;
    }
}
