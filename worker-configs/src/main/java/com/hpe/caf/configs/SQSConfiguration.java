/*
 * Copyright 2015-2024 Open Text.
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
package com.hpe.caf.configs;

import com.hpe.caf.api.Encrypted;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SQSConfiguration
{
    @NotNull
    @Size(min = 1)
    private String sqsProtocol;

    @NotNull
    @Size(min = 1)
    private String sqsHost;

    @Min(1024)
    @Max(65535)
    private int sqsPort;

    @NotNull
    @Size(min = 1)
    private String sqsRegion;

    @NotNull
    @Size(min = 1)
    private String sqsAccessKey;

    @Encrypted
    @NotNull
    @Size(min = 1)
    private String sqsSecretAccessKey;

    public String getSqsProtocol()
    {
        return sqsProtocol;
    }

    public void setSqsProtocol(final String sqsProtocol)
    {
        this.sqsProtocol = sqsProtocol;
    }

    public String getSqsHost()
    {
        return sqsHost;
    }

    public void setSqsHost(final String sqsHost)
    {
        this.sqsHost = sqsHost;
    }

    public int getSqsPort()
    {
        return sqsPort;
    }

    public void setSqsPort(final int sqsPort)
    {
        this.sqsPort = sqsPort;
    }

    public String getSqsRegion()
    {
        return sqsRegion;
    }

    public void setSqsRegion(final String sqsRegion)
    {
        this.sqsRegion = sqsRegion;
    }

    public String getSqsAccessKey()
    {
        return sqsAccessKey;
    }

    public void setSqsAccessKey(final String sqsAccessKey)
    {
        this.sqsAccessKey = sqsAccessKey;
    }

    public String getSqsSecretAccessKey()
    {
        return sqsSecretAccessKey;
    }

    public void setSqsSecretAccessKey(final String sqsSecretAccessKey)
    {
        this.sqsSecretAccessKey = sqsSecretAccessKey;
    }

    public String getURIString()
    {
        return sqsProtocol + "://" + sqsHost + ":" + sqsPort;
    }
}
