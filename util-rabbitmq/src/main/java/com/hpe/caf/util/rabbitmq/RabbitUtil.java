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
package com.hpe.caf.util.rabbitmq;

import com.hpe.caf.configs.RabbitConfiguration;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ExceptionHandler;
import com.rabbitmq.client.RecoveryDelayHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
     * Create a new RabbitMQ connection with a default configuration.
     *
     * @param host the host or IP running RabbitMQ
     * @param port the port that the RabbitMQ server is exposed on
     * @param user the username to use when authenticating with RabbitMQ
     * @param pass the password to use when authenticating with RabbitMQ
     * @return a valid connection to RabbitMQ
     * @throws IOException if the connection fails to establish
     * @throws TimeoutException if the connection fails to establish
     */
    public static Connection createRabbitConnection(String protocol, String host, int port, String user, String pass)
        throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException
    {
        final RabbitConfiguration rc = new RabbitConfiguration();
        rc.setRabbitProtocol(protocol);
        rc.setRabbitHost(host);
        rc.setRabbitPort(port);
        rc.setRabbitUser(user);
        rc.setRabbitPassword(pass);
        rc.setMaxBackoffInterval(30);
        rc.setBackoffInterval(1);
        rc.setMaxAttempts(20);
        return createRabbitConnection(rc);
    }

    /**
     * Create a new RabbitMQ connection with custom settings.
     *
     * @param rc the connection config
     * @return a valid connection to RabbitMQ
     * @throws IOException if the connection fails to establish
     * @throws TimeoutException if the connection fails to establish
     */
    public static Connection createRabbitConnection(final RabbitConfiguration rc)
            throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException
    {
        return createRabbitConnection(rc, null);
    }

    /**
     * Create a new RabbitMQ connection with custom settings.
     *
     * @param rc the connection config
     * @param exceptionHandler the exception handling implementation, a default handler will be used if null.
     * @return a valid connection to RabbitMQ
     * @throws IOException if the connection fails to establish
     * @throws TimeoutException if the connection fails to establish
     */
    public static Connection createRabbitConnection(final RabbitConfiguration rc,
                                                    final ExceptionHandler exceptionHandler)
            throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException
    {
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(rc.getRabbitUser());
        factory.setPassword(rc.getRabbitPassword());

        final URI rabbitUrl = new URI(String.format("%s://%s:%s", rc.getRabbitProtocol(), rc.getRabbitHost(), 
                rc.getRabbitPort()));
        factory.setUri(rabbitUrl);

        if (exceptionHandler != null) {
            factory.setExceptionHandler(exceptionHandler);
        }
        final List<Long> backOff = new ArrayList<>();
        long backOffCount = rc.getBackoffInterval();
        backOff.add(backOffCount);
        while (backOffCount < rc.getMaxBackoffInterval()) {
            backOffCount += rc.getBackoffInterval();
            backOff.add(backOffCount);
        }
        factory.setRecoveryDelayHandler(new RecoveryDelayHandler.ExponentialBackoffDelayHandler(backOff));
        factory.setAutomaticRecoveryEnabled(true);
        return factory.newConnection();
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
        declareWorkerQueue(channel, queueName, 0, QueueCreator.RABBIT_PROP_QUEUE_TYPE_QUORUM);
    }

    /**
     * Ensure a queue for a worker has been declared. Both a consumer *and* a publisher should call this before they attempt to use a
     * worker queue for the first time.
     *
     * @param channel the channel to use to declare the queue
     * @param queueName the name of the worker queue
     * @param maxPriority the maximum supported priority, pass 0 to disable priority
     * @param queueType the type of queue to be created eg: classic or quorum
     * @throws IOException if the queue is not valid and cannot be used, this is likely NOT retryable
     */
    public static void declareWorkerQueue(Channel channel, String queueName, int maxPriority, String queueType)
        throws IOException
    {
        final Map<String, Object> args = new HashMap<>();
        if (maxPriority > 0) {
            if (Objects.equals(queueType, QueueCreator.RABBIT_PROP_QUEUE_TYPE_CLASSIC)) {
                LOG.trace("Setting up priority to: {}", maxPriority);
                args.put(QueueCreator.RABBIT_PROP_KEY_MAX_PRIORITY, maxPriority);
            }
            else {
                LOG.warn("Priority is not supported with {} queues", queueType);
            }
        }
        args.put(QueueCreator.RABBIT_PROP_QUEUE_TYPE, queueType);
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
