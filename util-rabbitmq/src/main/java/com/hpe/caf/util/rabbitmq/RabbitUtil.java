/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.hpe.caf.util.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import net.jodah.lyra.ConnectionOptions;
import net.jodah.lyra.Connections;
import net.jodah.lyra.config.Config;
import net.jodah.lyra.config.RecoveryPolicy;
import net.jodah.lyra.config.RetryPolicy;
import net.jodah.lyra.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * Utility wrapper methods for interacting with RabbitMQ.
 */
public final class RabbitUtil
{
    private static final Logger LOG = LoggerFactory.getLogger(RabbitUtil.class);

    private RabbitUtil()
    {
    }

    /**
     * Create a new Lyra managed RabbitMQ connection with a default Lyra configuration.
     *
     * @param host the host or IP running RabbitMQ
     * @param port the port that the RabbitMQ server is exposed on
     * @param user the username to use when authenticating with RabbitMQ
     * @param pass the password to use when autenticating with RabbitMQ
     * @return a valid connection to RabbitMQ, managed by Lyra
     * @throws IOException if the connection fails to establish
     * @throws TimeoutException if the connection fails to establish
     */
    public static Connection createRabbitConnection(String host, int port, String user, String pass)
        throws IOException, TimeoutException
    {
        return createRabbitConnection(createLyraConnectionOptions(host, port, user, pass), createLyraConfig(1, 30, 20));
    }

    /**
     * Create a new Lyra managed RabbitMQ connection with custom settings.
     *
     * @param opts the Lyra ConnectionOptions
     * @param config the Lyra Config
     * @return a valid connection to RabbitMQ, managed by Lyra
     * @throws IOException if the connection fails to establish
     * @throws TimeoutException if the connection fails to establish
     */
    public static Connection createRabbitConnection(ConnectionOptions opts, Config config)
        throws IOException, TimeoutException
    {
        return Connections.create(opts, config);
    }

    /**
     * Generate a pre-populated Lyra ConnectionOptions object which can be used together with a Lyra Config object to establish a RabbitMQ
     * connection. If you wish to use defaults, just call the createRabbitConnection(RabbitConfiguration) method.
     *
     * @param host the host or IP running RabbitMQ
     * @param port the port that the RabbitMQ server is exposed on
     * @param user the username to use when authenticating with RabbitMQ
     * @param pass the password to use when autenticating with RabbitMQ
     * @return a Lyra ConnectionOptions object with settings configured from the RabbitConfiguration specified
     */
    public static ConnectionOptions createLyraConnectionOptions(String host, int port, String user, String pass)
    {
        return new ConnectionOptions().withHost(host).withPort(port).withUsername(user).withPassword(pass);
    }

    /**
     * Generate a pre-populated Lyra Config object which can be used together with a Lyra ConnectionOptions object to establish a RabbitMQ
     * connection. If you wish to use defaults, just call the createRabbitConnection(RabbitConfiguration) method.
     *
     * @param backoffInterval the initial interval, in seconds, between re-attempts upon failed RabbitMQ operations
     * @param maxBackoffInterval the maximum interval, in seconds, between re-attempts upon failed RabbitMQ operations
     * @param maxAttempts the maximum number of attempts to retry failed RabbitMQ operations, -1 is unlimited
     * @return a Lyra Config object with settings configured from the RabbitConfiguration specified
     */
    public static Config createLyraConfig(int backoffInterval, int maxBackoffInterval, int maxAttempts)
    {
        RecoveryPolicy recoveryPolicy
            = new RecoveryPolicy().withBackoff(Duration.seconds(backoffInterval), Duration.seconds(maxBackoffInterval));
        RetryPolicy retryPolicy
            = new RetryPolicy().withBackoff(Duration.seconds(backoffInterval), Duration.seconds(maxBackoffInterval)).withMaxAttempts(maxAttempts);
        return new Config().withRetryPolicy(retryPolicy).withRecoveryPolicy(recoveryPolicy.withMaxAttempts(maxAttempts));
    }

    /**
     * Ensure a queue for a worker has been declared. Both a consumer *and* a publisher should call this before they attempt to use a
     * worker queue for the first time.
     *
     * @param channel the channel to use to declare the queue
     * @param queueName the name of the worker queue
     * @throws IOException if the queue is not valid and cannot be used, this is likely NOT retryable
     */
    public static void declareWorkerQueue(Channel channel, String queueName)
        throws IOException
    {
        declareWorkerQueue(channel, queueName, 0);
    }

    /**
     * Ensure a queue for a worker has been declared. Both a consumer *and* a publisher should call this before they attempt to use a
     * worker queue for the first time.
     *
     * @param channel the channel to use to declare the queue
     * @param queueName the name of the worker queue
     * @param maxPriority the maximum supported priority, pass 0 to disable priority
     * @throws IOException if the queue is not valid and cannot be used, this is likely NOT retryable
     */
    public static void declareWorkerQueue(Channel channel, String queueName, int maxPriority)
        throws IOException
    {
        Map<String, Object> args = new HashMap<>();
        if (maxPriority > 0) {
            LOG.trace("Setting up priority to: {}", maxPriority);
            args.put(QueueCreator.RABBIT_PROP_KEY_MAX_PRIORITY, maxPriority);
        }
        declareQueue(channel, queueName, Durability.DURABLE, Exclusivity.NON_EXCLUSIVE, EmptyAction.LEAVE_EMPTY, args);
    }

    /**
     * Declare a queue with arbitrary parameters and default queue properties.
     *
     * @param channel the channel to use to declare the queue
     * @param queueName the name of the queue
     * @param dur the durability setting of the queue
     * @param excl the exclusivity setting of the queue
     * @param act the empty action setting of the queue
     * @throws IOException if the queue already exists AND the parameter settings do not match the existing queue
     */
    public static void declareQueue(Channel channel, String queueName, Durability dur, Exclusivity excl, EmptyAction act)
        throws IOException
    {
        declareQueue(channel, queueName, dur, excl, act, Collections.emptyMap());
    }

    /**
     * Declare a queue with arbitrary parameters and properties.
     *
     * @param channel the channel to use to declare the queue
     * @param queueName the name of the queue
     * @param dur the durability setting of the queue
     * @param excl the exclusivity setting of the queue
     * @param act the empty action setting of the queue
     * @param queueProps the queue properties map
     * @throws IOException if the queue already exists AND the parameter settings do not match the existing queue
     */
    public static void declareQueue(Channel channel, String queueName, Durability dur, Exclusivity excl, EmptyAction act, Map<String, Object> queueProps)
        throws IOException
    {
        Objects.requireNonNull(queueName);
        Objects.requireNonNull(dur);
        Objects.requireNonNull(excl);
        Objects.requireNonNull(act);
        Objects.requireNonNull(queueProps);
        try {
            channel.queueDeclare(queueName, dur == Durability.DURABLE, excl == Exclusivity.EXCLUSIVE, act == EmptyAction.AUTO_REMOVE, queueProps);
        } catch (IOException e) {
            LOG.warn("IO Exception encountered during queueDeclare. Will try do declare passively.", e);
            channel.queueDeclarePassive(queueName);
        }
    }
}
