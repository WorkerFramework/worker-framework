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

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

/**
 * Created by ploch on 08/11/2015.
 */
public class QueueManager implements Closeable {

    private final QueueServices queueServices;
    private final WorkerServices workerServices;
    private String consumerTag;

    private Channel pubChan;
    private Channel conChan;

    private DefaultRabbitConsumer rabbitConsumer;
    private Connection connection;

    public QueueManager(QueueServices queueServices, WorkerServices workerServices) {

        this.queueServices = queueServices;
        this.workerServices = workerServices;
    }

    public Thread start(ResultHandler resultHandler) throws IOException {

        connection = queueServices.getConnection();

        pubChan = connection.createChannel();
        conChan = connection.createChannel();

        RabbitUtil.declareWorkerQueue(pubChan, queueServices.getWorkerInputQueue());
        RabbitUtil.declareWorkerQueue(conChan, queueServices.getWorkerResultsQueue());


        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();

        SimpleQueueConsumerImpl queueConsumer = new SimpleQueueConsumerImpl(conEvents, conChan, resultHandler);
        rabbitConsumer = new DefaultRabbitConsumer(conEvents, queueConsumer);

        consumerTag = conChan.basicConsume(queueServices.getWorkerResultsQueue(), rabbitConsumer);
        Thread consumerThread = new Thread(rabbitConsumer);
       // resultHandler.setContext(new ExecutionContext(consumerThread));
        consumerThread.start();
        return consumerThread;
        //return consumerThread;
    }

    public void publish(TaskMessage message) throws CodecException, IOException {

        byte[] data = workerServices.getCodec().serialise(message);
        pubChan.basicPublish("", queueServices.getWorkerInputQueue(), MessageProperties.TEXT_PLAIN, data);

    }

    @Override
    public void close() throws IOException {
        if ( consumerTag != null ) {
            try {
                conChan.basicCancel(consumerTag);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if ( rabbitConsumer != null ) {
            rabbitConsumer.shutdown();
        }
        if ( conChan != null ) {
            try {
                conChan.close();
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        }
        if ( pubChan != null ) {
            try {
                pubChan.close();
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        }
        if ( connection != null ) {
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
