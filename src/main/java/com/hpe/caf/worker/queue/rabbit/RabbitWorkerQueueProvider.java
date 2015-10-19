package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.WorkerQueueProvider;


/**
 * {@inheritDoc}
 */
public class RabbitWorkerQueueProvider implements WorkerQueueProvider
{
    /**
     * {@inheritDoc}
     */
    @Override
    public ManagedWorkerQueue getWorkerQueue(final ConfigurationSource configurationSource, final int maxTasks)
            throws QueueException
    {
        try {
            return new RabbitWorkerQueue(configurationSource.getConfiguration(RabbitWorkerQueueConfiguration.class), maxTasks);
        } catch (ConfigurationException e) {
            throw new QueueException("Cannot create worker queue", e);
        }
    }
}
