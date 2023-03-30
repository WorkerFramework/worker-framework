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
package com.hpe.caf.worker.datastore.s3;

import com.hpe.caf.api.Configuration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Configuration
public class S3DataStoreConfiguration
{
    @NotNull
    @Size(min = 1)
    private String accessKey;

    @NotNull
    @Size(min = 1)
    private String secretKey;

    @NotNull
    @Size(min = 1)
    private String bucketName;

    private String proxyProtocol;

    private String proxyHost;

    @Min(1)
    @Max(65535)
    private int proxyPort;

    public S3DataStoreConfiguration()
    {
    }

    public String getAccessKey()
    {
        return accessKey;
    }

    public void setAccessKey(String accessKey)
    {
        this.accessKey = accessKey;
    }

    public String getSecretKey()
    {
        return secretKey;
    }

    public void setSecretKey(String secretKey)
    {
        this.secretKey = secretKey;
    }

    public String getBucketName()
    {
        return bucketName;
    }

    public void setBucketName(String bucketName)
    {
        this.bucketName = bucketName;
    }

    public String getProxyProtocol()
    {
        return proxyProtocol;
    }

    public void setProxyProtocol(String proxyProtocol)
    {
        this.proxyProtocol = proxyProtocol;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost)
    {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort()
    {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort)
    {
        this.proxyPort = proxyPort;
    }
}
