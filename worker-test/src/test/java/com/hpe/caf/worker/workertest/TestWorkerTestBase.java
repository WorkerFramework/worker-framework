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
package com.hpe.caf.worker.workertest;

import com.google.common.base.Strings;
import com.rabbitmq.client.ConnectionFactory;

public class TestWorkerTestBase {
    protected ConnectionFactory connectionFactory;
    private static final String CAF_RABBITMQ_HOST = "CAF_RABBITMQ_HOST";
    private static final String CAF_RABBITMQ_PORT = "CAF_RABBITMQ_PORT";
    private static final String CAF_RABBITMQ_USERNAME = "CAF_RABBITMQ_USERNAME";
    private static final String CAF_RABBITMQ_PASSWORD = "CAF_RABBITMQ_PASSWORD";
    private static final String CAF_RABBITMQ_CTRL_PORT = "CAF_RABBITMQ_CTRL_PORT";

    public TestWorkerTestBase() {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(getEnvOrDefault(CAF_RABBITMQ_HOST, "localhost"));
        connectionFactory.setPort(Integer.parseInt(getEnvOrDefault(CAF_RABBITMQ_PORT, "25672")));
        connectionFactory.setUsername(getEnvOrDefault(CAF_RABBITMQ_USERNAME, "guest"));
        connectionFactory.setPassword(getEnvOrDefault(CAF_RABBITMQ_PASSWORD, "guest"));
        connectionFactory.setVirtualHost("/");
    }

    private static String getEnvOrDefault(final String name, final String defaultValue) {
        final String value = System.getenv(name);

        return !Strings.isNullOrEmpty(value) ? value : defaultValue;
    }
}
