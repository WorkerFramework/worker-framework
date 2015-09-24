package com.hpe.caf.util.rabbitmq;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import net.jodah.lyra.ConnectionOptions;
import net.jodah.lyra.Connections;
import net.jodah.lyra.config.Config;
import net.jodah.lyra.config.RecoveryPolicy;
import net.jodah.lyra.util.Duration;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeoutException;


/**
 * Utility wrapper methods for interacting with RabbitMQ.
 * @since 6.0
 */
public final class RabbitUtil
{
    private RabbitUtil() { }


    /**
     * Create a new Lyra managed RabbitMQ connection.
     * @param conf contains the necessary Lyra and RabbitMQ configuration
     * @return a valid connection to RabbitMQ, managed by Lyra
     * @throws IOException if the connection fails to establish
     * @throws TimeoutException if the connection fails to establish
     */
    public static Connection createRabbitConnection(final RabbitConfiguration conf)
        throws IOException, TimeoutException
    {
        return createRabbitConnection(createLyraConnectionOptions(conf), createLyraConfig(conf));
    }


    /**
     * Create a new Lyra managed RabbitMQ connection with custom settings.
     * @param opts the Lyra ConnectionOptions
     * @param config the Lyra Config
     * @return a valid connection to RabbitMQ, managed by Lyra
     * @throws IOException if the connection fails to establish
     * @throws TimeoutException if the connection fails to establish
     * @since 7.0
     */
    public static Connection createRabbitConnection(final ConnectionOptions opts, final Config config)
        throws IOException, TimeoutException
    {
        return Connections.create(opts, config);
    }


    /**
     * Generate a pre-populated Lyra ConnectionOptions object which can be used together with a
     * Lyra Config object to establish a RabbitMQ connection. If you wish to use defaults, just
     * call the createRabbitConnection(RabbitConfiguration) method.
     * @param conf contains the necessary Lyra and RabbitMQ configuration
     * @return a Lyra ConnectionOptions object with settings configured from the RabbitConfiguration specified
     * @since 7.0
     */
    public static ConnectionOptions createLyraConnectionOptions(final RabbitConfiguration conf)
    {
        return new ConnectionOptions().withHost(conf.getRabbitHost()).withPort(conf.getRabbitPort())
                                      .withUsername(conf.getRabbitUser()).withPassword(conf.getRabbitPassword());
    }


    /**
     * Generate a pre-populated Lyra Config object which can be used together with a Lyra
     * ConnectionOptions object to establish a RabbitMQ connection. If you wish to use defaults, just
     * call the createRabbitConnection(RabbitConfiguration) method.
     * @param conf contains the necessary Lyra and RabbitMQ configuration
     * @return a Lyra Config object with settings configured from the RabbitConfiguration specified
     * @since 7.0
     */
    public static Config createLyraConfig(final RabbitConfiguration conf)
    {
        RecoveryPolicy policy =
            new RecoveryPolicy().withBackoff(Duration.seconds(conf.getBackoffInterval()), Duration.seconds(conf.getMaxBackoffInterval()));
        return new Config().withRecoveryPolicy(policy.withMaxAttempts(conf.getMaxAttempts()));
    }


    /**
     * Ensure a queue for a worker has been declared. Both a consumer *and* a publisher should call this before they
     * attempt to use a worker queue for the first time.
     * @param channel the channel to use to declare the queue
     * @param queueName the name of the worker queue
     * @throws IOException if the queue is not valid and cannot be used, this is likely NOT retryable
     */
    public static void declareWorkerQueue(final Channel channel, final String queueName)
        throws IOException
    {
        declareQueue(channel, queueName, Durability.DURABLE, Exclusivity.NON_EXCLUSIVE, EmptyAction.LEAVE_EMPTY);
    }


    /**
     * Declare a queue with arbitrary parameters.
     * @param channel the channel to use to declare the queue
     * @param queueName the name of the queue
     * @param dur the durability setting of the queue
     * @param excl the exclusivity setting of the queue
     * @param act the empty action setting of the queue
     * @throws IOException if the queue already exists AND the parameter settings do not match the existing queue
     */
    public static void declareQueue(final Channel channel, final String queueName, final Durability dur, final Exclusivity excl, final EmptyAction act)
        throws IOException
    {
        channel.queueDeclare(queueName, dur == Durability.DURABLE, excl == Exclusivity.EXCLUSIVE, act == EmptyAction.AUTO_REMOVE, Collections.emptyMap());
    }
}
