package com.hpe.caf.worker.testing;

import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.configs.RabbitConfiguration;
import com.hpe.caf.util.rabbitmq.RabbitUtil;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;
import com.hpe.caf.worker.queue.rabbit.WorkerConnectionListener;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import net.jodah.lyra.ConnectionOptions;
import net.jodah.lyra.config.Config;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by ploch on 08/11/2015.
 */
public class QueueServicesFactory {

    private static class NoOpCallback implements TaskCallback {

        @Override
        public void registerNewTask(String s, byte[] bytes) throws TaskRejectedException, InvalidTaskException {
        }

        @Override
        public void abortTasks() {
        }
    }

    public static QueueServices create(final RabbitWorkerQueueConfiguration configuration, final String resultsQueueName) throws IOException, TimeoutException {
        Connection connection = createConnection(configuration, new NoOpCallback());
        Channel pubChan = connection.createChannel();
        Channel conChan = connection.createChannel();

        RabbitUtil.declareWorkerQueue(pubChan, configuration.getInputQueue());
        RabbitUtil.declareWorkerQueue(conChan, resultsQueueName);

      //  conChan.basicConsume(resultsQueueName, new DefaultRabbitConsumer(new LinkedBlockingQueue<Event<QueueConsumer>>(), new SimpleQueueConsumerImpl()));


        return new QueueServices(connection, pubChan, configuration.getInputQueue(), conChan, resultsQueueName);
    }

    private static Connection createConnection(RabbitWorkerQueueConfiguration configuration, final TaskCallback callback)
            throws IOException, TimeoutException
    {
        RabbitConfiguration rc = configuration.getRabbitConfiguration();
        ConnectionOptions lyraOpts = RabbitUtil.createLyraConnectionOptions(rc.getRabbitHost(), rc.getRabbitPort(), rc.getRabbitUser(), rc.getRabbitPassword());
        Config lyraConfig = RabbitUtil.createLyraConfig(rc.getBackoffInterval(), rc.getMaxBackoffInterval(), rc.getMaxAttempts());
        lyraConfig.withConnectionListeners(new WorkerConnectionListener(callback));
        Connection connection = RabbitUtil.createRabbitConnection(lyraOpts, lyraConfig);
        return connection;
    }
}
