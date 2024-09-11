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

import com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper;
import com.hpe.caf.worker.queue.sqs.util.WrapperConfig;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

public class TestContainer
{
    protected static LocalStackContainer container;

    // Just allows to debug in IDE
    private static final String imageName = getEnvOrDefault(
            "LOCALSTACK_IMAGE",
            "worker-framework-8.2.0-SNAPSHOT.project-registries.local/localstack/localstack:latest"

    );

    // Just allows to debug in IDE
    private static final String compatibleImageName = getEnvOrDefault(
            "COMPATIBLE_LOCALSTACK_IMAGE",
            "localstack/localstack"

    );

    static {
        if (container == null || !container.isRunning()) {
            container = new LocalStackContainer(
                    DockerImageName.parse(imageName)
                    .asCompatibleSubstituteFor(compatibleImageName)
            );
            container.withServices(LocalStackContainer.Service.SQS, LocalStackContainer.Service.CLOUDWATCH)
                    .withEnv("LS_LOG", "error")
                     .start();
        }
    }

    public static WorkerQueueWrapper getWorkerWrapper(final String inputQueue)
    {
        return getWorkerWrapper(inputQueue, inputQueue);
    }

    public static WorkerQueueWrapper getWorkerWrapper(final String inputQueue, final String retryQueue)
    {
        return WorkerQueueWrapper.getWorkerWrapper(container, inputQueue, retryQueue);
    }

    public static WorkerQueueWrapper getWorkerWrapper(
            final String inputQueue,
            final String retryQueue,
            final WrapperConfig wrapperConfig)
    {
        return WorkerQueueWrapper.getWorkerWrapper(container, inputQueue, retryQueue, wrapperConfig);
    }

    private static String getEnvOrDefault(final String envName, final String defaultValue)
    {
        final var value = System.getenv(envName);
        return value != null ? value : defaultValue;
    }
}
