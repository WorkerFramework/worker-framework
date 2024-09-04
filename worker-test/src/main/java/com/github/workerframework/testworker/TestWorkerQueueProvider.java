package com.github.workerframework.testworker;

import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.WorkerQueueProvider;

public class TestWorkerQueueProvider implements WorkerQueueProvider
{
    @Override
    public ManagedWorkerQueue getWorkerQueue(ConfigurationSource configurationSource, int maxTasks) throws QueueException
    {
        return null;
    }
}
