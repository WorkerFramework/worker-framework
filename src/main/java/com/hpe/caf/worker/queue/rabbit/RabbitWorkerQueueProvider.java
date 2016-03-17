package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.WorkerQueueProvider;


public class RabbitWorkerQueueProvider implements WorkerQueueProvider
{
    @Override
    public ManagedWorkerQueue getWorkerQueue(final ConfigurationSource configurationSource, final int maxTasks, final Codec codec)
            throws QueueException
    {
        try {
            return new RabbitWorkerQueue(configurationSource.getConfiguration(RabbitWorkerQueueConfiguration.class), maxTasks, codec);
        } catch (ConfigurationException e) {
            throw new QueueException("Cannot create worker queue", e);
        }
    }
}
