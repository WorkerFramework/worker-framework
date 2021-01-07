/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.testing;

import com.hpe.caf.api.Codec;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

/**
 * Created by ploch on 08/11/2015.
 */
public class QueueServices
{
    private final Connection connection;
    private final Channel publisherChannel;
    private final String workerInputQueue;
    private final Channel consumerChannel;
    private final String workerResultsQueue;
    private final Codec codec;
    private final int maxPriority;

    public QueueServices(Connection connection, Channel publisherChannel, String workerInputQueue, Channel consumerChannel, String workerResultsQueue, Codec codec, int maxPriority)
    {

        this.connection = connection;
        this.publisherChannel = publisherChannel;
        this.workerInputQueue = workerInputQueue;
        this.consumerChannel = consumerChannel;
        this.workerResultsQueue = workerResultsQueue;
        this.codec = codec;
        this.maxPriority = maxPriority;
    }

    /**
     * Getter for property 'connection'.
     *
     * @return Value for property 'connection'.
     */
    public Connection getConnection()
    {
        return connection;
    }

    /**
     * Getter for property 'publisherChannel'.
     *
     * @return Value for property 'publisherChannel'.
     */
    public Channel getPublisherChannel()
    {
        return publisherChannel;
    }

    /**
     * Getter for property 'workerInputQueue'.
     *
     * @return Value for property 'workerInputQueue'.
     */
    public String getWorkerInputQueue()
    {
        return workerInputQueue;
    }

    /**
     * Getter for property 'consumerChannel'.
     *
     * @return Value for property 'consumerChannel'.
     */
    public Channel getConsumerChannel()
    {
        return consumerChannel;
    }

    /**
     * Getter for property 'workerResultsQueue'.
     *
     * @return Value for property 'workerResultsQueue'.
     */
    public String getWorkerResultsQueue()
    {
        return workerResultsQueue;
    }

    /**
     * Getter for property 'codec'.
     *
     * @return Value for property 'codec'.
     */
    public Codec getCodec()
    {
        return codec;
    }

    /**
     * Getter for property 'maxPriority'.
     *
     * @return Value for property 'maxPriority'.
     */
    public int getMaxPriority()
    {
        return maxPriority;
    }

    /*public void publish(TaskMessage taskMessage) throws CodecException, IOException {


        byte[] data = codec.serialise(taskMessage);
        publisherChannel.basicPublish("", workerInputQueue, MessageProperties.TEXT_PLAIN, data);
    }*/
}
