/*
 * Copyright 2015-2023 Open Text.
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
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 10000;
    public static final int DEFAULT_READ_TIMEOUT_MILLIS = 10000;

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

    @SuppressWarnings("null") // Not unboxing null value, checked before returning
    public Integer getConnectTimeoutMillis()
    {
        return (connectTimeoutMillis == null || connectTimeoutMillis <= 0) ? DEFAULT_CONNECT_TIMEOUT_MILLIS : connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(final Integer connectTimeoutMillis)
    {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    @SuppressWarnings("null") // Not unboxing null value, checked before returning
    public Integer getReadTimeoutMillis()
    {
        return (readTimeoutMillis == null || readTimeoutMillis <= 0) ? DEFAULT_READ_TIMEOUT_MILLIS : readTimeoutMillis;
    }

    public void setReadTimeoutMillis(final int readTimeoutMillis)
    {
        this.readTimeoutMillis = readTimeoutMillis;
    }
}
