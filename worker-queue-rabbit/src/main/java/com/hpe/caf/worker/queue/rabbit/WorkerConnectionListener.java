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
 * @since 7.5
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
