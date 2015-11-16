package com.hpe.caf.worker.testing;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.configs.RabbitConfiguration;
import com.hpe.caf.util.rabbitmq.RabbitUtil;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;
import com.hpe.caf.worker.queue.rabbit.WorkerConnectionListener;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import net.jodah.lyra.ConnectionOptions;
import net.jodah.lyra.config.Config;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by ploch on 08/11/2015.
 */
public class QueueServices {

    private final Connection connection;
    private final Channel publisherChannel;
    private final String workerInputQueue;
    private final Channel consumerChannel;
    private final String workerResultsQueue;
    private final Codec codec;

    public QueueServices(Connection connection, Channel publisherChannel, String workerInputQueue, Channel consumerChannel, String workerResultsQueue) {
        this(connection, publisherChannel, workerInputQueue, consumerChannel, workerResultsQueue, new JsonCodec());
    }

    public QueueServices(Connection connection, Channel publisherChannel, String workerInputQueue, Channel consumerChannel, String workerResultsQueue, Codec codec) {

        this.connection = connection;
        this.publisherChannel = publisherChannel;
        this.workerInputQueue = workerInputQueue;
        this.consumerChannel = consumerChannel;
        this.workerResultsQueue = workerResultsQueue;
        this.codec = codec;
    }

    /**
     * Getter for property 'connection'.
     *
     * @return Value for property 'connection'.
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Getter for property 'publisherChannel'.
     *
     * @return Value for property 'publisherChannel'.
     */
    public Channel getPublisherChannel() {
        return publisherChannel;
    }

    /**
     * Getter for property 'workerInputQueue'.
     *
     * @return Value for property 'workerInputQueue'.
     */
    public String getWorkerInputQueue() {
        return workerInputQueue;
    }

    /**
     * Getter for property 'consumerChannel'.
     *
     * @return Value for property 'consumerChannel'.
     */
    public Channel getConsumerChannel() {
        return consumerChannel;
    }

    /**
     * Getter for property 'workerResultsQueue'.
     *
     * @return Value for property 'workerResultsQueue'.
     */
    public String getWorkerResultsQueue() {
        return workerResultsQueue;
    }

    /**
     * Getter for property 'codec'.
     *
     * @return Value for property 'codec'.
     */
    public Codec getCodec() {
        return codec;
    }

    /*public void publish(TaskMessage taskMessage) throws CodecException, IOException {


        byte[] data = codec.serialise(taskMessage);
        publisherChannel.basicPublish("", workerInputQueue, MessageProperties.TEXT_PLAIN, data);
    }*/


}
