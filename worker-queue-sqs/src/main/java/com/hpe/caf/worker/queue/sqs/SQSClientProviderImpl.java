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

import com.hpe.caf.worker.queue.sqs.config.SQSConfiguration;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;
import java.net.URISyntaxException;

public final class SQSClientProviderImpl implements SQSClientProvider
{
    private final SQSConfiguration sqsConfiguration;

    public SQSClientProviderImpl(SQSConfiguration sqsConfiguration)
    {
        this.sqsConfiguration = sqsConfiguration;
    }

    @Override
    public SqsClient getSqsClient() throws URISyntaxException
    {
        return SqsClient.builder()
                .endpointOverride(new URI(sqsConfiguration.getURIString()))
                .region(Region.of(sqsConfiguration.getAwsRegion()))
                .credentialsProvider(this::getAWSCredentials)
                .build();
    }

    public AwsCredentials getAWSCredentials()
    {
        return new AwsCredentials()
        {
            @Override
            public String accessKeyId()
            {
                return sqsConfiguration.getAwsAccessKey();
            }

            @Override
            public String secretAccessKey()
            {
                return sqsConfiguration.getSecretAccessKey();
            }
        };
    }
}
