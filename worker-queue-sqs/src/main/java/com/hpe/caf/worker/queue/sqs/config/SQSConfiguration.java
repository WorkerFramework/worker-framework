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
package com.hpe.caf.worker.queue.sqs.config;

import com.hpe.caf.api.Encrypted;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SQSConfiguration
{
    @NotNull
    @Size(min = 1)
    private String awsProtocol;

    @NotNull
    @Size(min = 1)
    private String awsHost;

    @Min(1024)
    @Max(65535)
    private int awsPort;

    @NotNull
    @Size(min = 1)
    private String awsRegion;

    @NotNull
    @Size(min = 1)
    private String awsAccessKey;

    @Encrypted
    @NotNull
    @Size(min = 1)
    private String secretAccessKey;

    public @NotNull @Size(min = 1) String getAwsProtocol() {
        return awsProtocol;
    }

    public void setAwsProtocol(@NotNull @Size(min = 1) String awsProtocol) {
        this.awsProtocol = awsProtocol;
    }

    public @NotNull @Size(min = 1) String getAwsHost() {
        return awsHost;
    }

    public void setAwsHost(@NotNull @Size(min = 1) String awsHost) {
        this.awsHost = awsHost;
    }

    @Min(1024)
    @Max(65535)
    public int getAwsPort() {
        return awsPort;
    }

    public void setAwsPort(@Min(1024) @Max(65535) int awsPort) {
        this.awsPort = awsPort;
    }

    public @NotNull @Size(min = 1) String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(@NotNull @Size(min = 1) String awsRegion) {
        this.awsRegion = awsRegion;
    }

    public @NotNull @Size(min = 1) String getAwsAccessKey() {
        return awsAccessKey;
    }

    public void setAwsAccessKey(@NotNull @Size(min = 1) String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
    }

    public @NotNull @Size(min = 1) String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(@NotNull @Size(min = 1) String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public String getURIString() {
        return awsProtocol + "://" + awsHost + ":" + awsPort;
    }
}
