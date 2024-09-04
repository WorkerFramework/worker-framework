package com.hpe.caf.worker.core;

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.WorkerQueueProvider;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueue;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;
import com.hpe.caf.worker.queue.sqs.SQSWorkerQueue;
import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;

public class WorkerQueueProviderImpl implements WorkerQueueProvider
{
    private static final String AWS_SQS_MESSAGING = "sqs";

    @Override
    public ManagedWorkerQueue getWorkerQueue(ConfigurationSource configurationSource, int maxTasks) throws QueueException
    {
        try {
            final var messageSystemCfg = configurationSource.getConfiguration(MessageSystemConfiguration.class);
            if (messageSystemCfg.getImplementation().equalsIgnoreCase(AWS_SQS_MESSAGING)) {
                return new SQSWorkerQueue(configurationSource.getConfiguration(SQSWorkerQueueConfiguration.class), maxTasks);
            }
            return new RabbitWorkerQueue(configurationSource.getConfiguration(RabbitWorkerQueueConfiguration.class), maxTasks);
        } catch (ConfigurationException e) {
            throw new QueueException("Cannot create worker queue", e);
        }
    }
}
