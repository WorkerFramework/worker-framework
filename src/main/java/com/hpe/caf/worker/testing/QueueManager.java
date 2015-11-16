package com.hpe.caf.worker.testing;

import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.util.rabbitmq.DefaultRabbitConsumer;
import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.util.rabbitmq.QueueConsumer;
import com.hpe.caf.util.rabbitmq.RabbitUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by ploch on 08/11/2015.
 */
public class QueueManager {

    private final QueueServices queueServices;
    private final WorkerServices workerServices;
    private String consumerTag;

    private Channel pubChan;
    private Channel conChan;


    public QueueManager(QueueServices queueServices, WorkerServices workerServices) {

        this.queueServices = queueServices;
        this.workerServices = workerServices;
    }

    public void start(ResultHandler resultHandler) throws IOException {

        Connection connection = queueServices.getConnection();

        pubChan = connection.createChannel();
        conChan = connection.createChannel();

        RabbitUtil.declareWorkerQueue(pubChan, queueServices.getWorkerInputQueue());
        RabbitUtil.declareWorkerQueue(conChan, queueServices.getWorkerResultsQueue());


        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();

        SimpleQueueConsumerImpl queueConsumer = new SimpleQueueConsumerImpl(conEvents, conChan, resultHandler);
        DefaultRabbitConsumer rabbitConsumer = new DefaultRabbitConsumer(conEvents, queueConsumer);

        consumerTag = conChan.basicConsume(queueServices.getWorkerResultsQueue(), rabbitConsumer);
        Thread consumerThread = new Thread(rabbitConsumer);
       // resultHandler.setContext(new ExecutionContext(consumerThread));
        consumerThread.start();
        //return consumerThread;
    }

    public void publish(TaskMessage message) throws CodecException, IOException {

        byte[] data = workerServices.getCodec().serialise(message);
        pubChan.basicPublish("", queueServices.getWorkerInputQueue(), MessageProperties.TEXT_PLAIN, data);

    }
}
