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
package com.opentext.caf.worker.workertest;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;

import java.io.IOException;
import java.util.Objects;

public class TestWorkerTestBase {
    final protected ConnectionFactory connectionFactory;
    private static final String CAF_RABBITMQ_HOST = "CAF_RABBITMQ_HOST";
    private static final String CAF_RABBITMQ_PORT = "CAF_RABBITMQ_PORT";
    private static final String CAF_RABBITMQ_USERNAME = "CAF_RABBITMQ_USERNAME";
    private static final String CAF_RABBITMQ_PASSWORD = "CAF_RABBITMQ_PASSWORD";

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

        return value != null && !Objects.equals(value, "") ? value : defaultValue;
    }
    public static class TestWorkerQueueConsumer implements Consumer {
        private byte[] lastDeliveredBody = null;
        @Override
        public void handleConsumeOk(String consumerTag) {

        }

        @Override
        public void handleCancelOk(String consumerTag) {

        }

        @Override
        public void handleCancel(String consumerTag) throws IOException {

        }

        @Override
        public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {

        }

        @Override
        public void handleRecoverOk(String consumerTag) {

        }

        public byte[] getLastDeliveredBody() {
            return lastDeliveredBody;
        }

        @Override
        public void handleDelivery(final String consumerTag, final Envelope envelope, final AMQP.BasicProperties properties,
                                   final byte[] body) throws IOException {
            lastDeliveredBody = body;
        }
    }
}
