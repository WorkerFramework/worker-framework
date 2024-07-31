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
package com.hpe.caf.worker.queue.sqs;

import com.hpe.caf.configs.SQSConfiguration;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.endpoints.SqsEndpointProvider;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

public class SqsClientProviderImpl implements SqsClientProvider
{
    @Override
    public SqsClient getSqsClient(final SQSConfiguration sqsConfiguration) throws URISyntaxException
    {
        return SqsClient.builder()
                .endpointOverride(new URI(sqsConfiguration.getURIString()))
                .region(Region.of(sqsConfiguration.getSqsRegion()))
                .credentialsProvider(() -> new AwsCredentials() {
                    @Override
                    public String accessKeyId() {
                        return sqsConfiguration.getSqsAccessKey();
                    }

                    @Override
                    public String secretAccessKey() {
                        return sqsConfiguration.getSqsSecretAccessKey();
                    }
                })
                .build();
    }

    private SqsEndpointProvider getEndpointProvider(final SQSConfiguration sqsConfiguration)
    {
        return endpointParams -> CompletableFuture.completedFuture(Endpoint.builder().url(URI.create(sqsConfiguration.getURIString())).build());
    }

    private static AwsCredentialsProvider getCredentialsProvider(final SQSConfiguration sqsConfiguration)
    {
        return new AwsCredentialsProvider()
        {
            @Override
            public AwsCredentials resolveCredentials()
            {
                return new AwsCredentials()
                {
                    @Override
                    public String accessKeyId()
                    {
                        return sqsConfiguration.getSqsAccessKey();
                    }

                    @Override
                    public String secretAccessKey()
                    {
                        return sqsConfiguration.getSqsSecretAccessKey();
                    }
                };
            }

            @Override
            public Class<AwsCredentialsIdentity> identityType()
            {
                return AwsCredentialsProvider.super.identityType();
            }

            @Override
            public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request)
            {
                return AwsCredentialsProvider.super.resolveIdentity(request);
            }
        };
    }
}
