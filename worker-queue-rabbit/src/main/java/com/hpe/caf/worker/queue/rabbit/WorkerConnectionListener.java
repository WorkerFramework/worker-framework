/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.queue.rabbit;

import com.hpe.caf.api.worker.TaskCallback;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Trivial ConnectionListener primarily to handle aborting in-progress tasks when the RabbitMQ connection is recovered - this is because
 * RabbitMQ will automatically re-queue the message when it detected our client dropped, and we don't want to produce a result for these
 * tasks running when the connection dropped to try and avoid duplicate results. This will also log all other events.
 */
public class WorkerConnectionListener implements RecoveryListener
{
    private final TaskCallback callback;
    private final WorkerConfirmListener confirmListener;
    private static final Logger LOG = LoggerFactory.getLogger(WorkerConnectionListener.class);

    public WorkerConnectionListener(TaskCallback taskCallback, WorkerConfirmListener listener)
    {
        this.callback = Objects.requireNonNull(taskCallback);
        this.confirmListener = Objects.requireNonNull(listener);
    }


    @Override
    public void handleRecovery(Recoverable recoverable)
    {
        LOG.info("Connection recovery completed, aborting all in-progress tasks");
        callback.abortTasks();
    }

    @Override
    public void handleRecoveryStarted(Recoverable recoverable)
    {
        LOG.info("Connection recovery starting");
        confirmListener.clearConfirmations();
    }
}
