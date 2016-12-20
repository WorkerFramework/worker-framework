/*
 * (c) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.api.worker.TaskCallback;
import com.rabbitmq.client.Connection;
import net.jodah.lyra.event.ConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


/**
 * Trivial ConnectionListener primarily to handle aborting in-progress tasks when
 * the RabbitMQ connection is recovered - this is because RabbitMQ will automatically
 * re-queue the message when it detected our client dropped, and we don't want to
 * produce a result for these tasks running when the connection dropped to try and
 * avoid duplicate results. This will also log all other events.
 */
public class WorkerConnectionListener implements ConnectionListener
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
    public void onCreate(final Connection connection)
    {
        LOG.debug("Connection created");
    }


    @Override
    public void onCreateFailure(final Throwable throwable)
    {
        LOG.debug("Failed to create connection");
    }


    @Override
    public void onRecoveryStarted(final Connection connection)
    {
        LOG.info("Connection recovery starting");
        confirmListener.clearConfirmations();
    }


    @Override
    public void onRecovery(final Connection connection)
    {
        LOG.info("Connection recovered");
    }


    @Override
    public void onRecoveryCompleted(final Connection connection)
    {
        LOG.info("Connection recovery completed, aborting all in-progress tasks");
        callback.abortTasks();
    }


    @Override
    public void onRecoveryFailure(final Connection connection, final Throwable throwable)
    {
        LOG.error("Connection failed to recover", throwable);
    }
}
