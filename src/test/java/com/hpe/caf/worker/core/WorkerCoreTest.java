package com.hpe.caf.worker.core;


import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.ServicePath;
import com.hpe.caf.api.worker.NewTaskCallback;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerQueue;
import com.hpe.caf.api.worker.WorkerQueueMetricsReporter;
import com.hpe.caf.api.worker.WorkerQueueProvider;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.codec.JsonCodec;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.naming.InvalidNameException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class WorkerCoreTest
{
    private static final byte[] SUCCESS_BYTES = "success".getBytes(StandardCharsets.UTF_8);
    private static final byte[] EXCEPTION_BYTES = "exception".getBytes(StandardCharsets.UTF_8);
    private static final String WORKER_NAME = "testWorker";
    private static final int WORKER_API_VER = 1;
    private static final String JOB_ID = "test1";
    private static final String QUEUE_OUT = "outQueue";
    private static final String SERVICE_PATH = "/test/group";


    @Test
    public void testWorkerCore()
        throws CodecException, InterruptedException, WorkerException, ConfigurationException, QueueException, InvalidNameException
    {
        BlockingQueue<byte[]> q = new LinkedBlockingQueue<>();
        BlockingQueue<byte[]> in = new LinkedBlockingQueue<>();
        Codec codec = new JsonCodec();
        ThreadPoolExecutor tpe = WorkerApplication.getDefaultThreadPoolExecutor(5);
        ConfigurationSource config = Mockito.mock(ConfigurationSource.class);
        ServicePath path = new ServicePath(SERVICE_PATH);
        WorkerCore core = new WorkerCore(codec, tpe, new TestWorkerQueueProvider(q, in).getWorkerQueue(config, 10), getWorkerFactory(), path);
        core.start();
        // at this point, the queue should hand off the task to the app, the app should get a worker from the mocked WorkerFactory,
        // and the Worker itself is a mock wrapped in a WorkerWrapper, which should return success and the appropriate result data
        TaskMessage tm = new TaskMessage();
        tm.setTaskId(WORKER_NAME);
        tm.setTaskStatus(TaskStatus.NEW_TASK);
        tm.setTaskClassifier(WORKER_NAME);
        tm.setTaskApiVersion(WORKER_API_VER);
        tm.setTaskData(codec.serialise(new TestWorkerJob()));
        byte[] stuff = codec.serialise(tm);
        in.offer(stuff);
        // the worker's task result should eventually be passed back to our dummy WorkerQueue and onto our blocking queue
        byte[] result = q.poll(5000, TimeUnit.MILLISECONDS);
        // if the result didn't get back to us, then result will be null
        Assert.assertNotNull(result);
        // deserialise and verify result data
        TaskMessage taskMessage = codec.deserialise(result, TaskMessage.class);
        Assert.assertEquals(TaskStatus.RESULT_SUCCESS, taskMessage.getTaskStatus());
        Assert.assertEquals(WORKER_NAME, taskMessage.getTaskClassifier());
        Assert.assertEquals(WORKER_API_VER, taskMessage.getTaskApiVersion());
        Assert.assertArrayEquals(SUCCESS_BYTES, taskMessage.getTaskData());
        Assert.assertTrue(taskMessage.getContext().containsKey(path.toString()));
        Assert.assertArrayEquals(SUCCESS_BYTES, taskMessage.getContext().get(path.toString()));
    }


    private WorkerFactory getWorkerFactory()
            throws WorkerException
    {
        WorkerFactory factory = Mockito.mock(WorkerFactory.class);
        Worker mockWorker = getWorker();
        Mockito.when(factory.getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(mockWorker);
        return factory;
    }


    private Worker getWorker()
    {
        return new Worker(QUEUE_OUT)
        {
            @Override
            public WorkerResponse doWork()
            {
                return createSuccessResult(SUCCESS_BYTES, SUCCESS_BYTES);
            }


            @Override
            public String getWorkerIdentifier()
            {
                return WORKER_NAME;
            }


            @Override
            public int getWorkerApiVersion()
            {
                return WORKER_API_VER;
            }


            @Override
            protected byte[] getGeneralFailureData()
            {
                return EXCEPTION_BYTES;
            }
        };
    }


    private class TestWorkerQueueProvider implements WorkerQueueProvider
    {
        private final BlockingQueue<byte[]> results;
        private final BlockingQueue<byte[]> incoming;


        public TestWorkerQueueProvider(final BlockingQueue<byte[]> results, final BlockingQueue<byte[]> incoming)
        {
            this.results = results;
            this.incoming = incoming;
        }


        @Override
        public final WorkerQueue getWorkerQueue(final ConfigurationSource configurationSource, final int maxTasks)
        {
            return new TestWorkerQueue(maxTasks, this.results, this.incoming);
        }
    }


    private class TestWorkerQueue extends WorkerQueue
    {
        private final BlockingQueue<byte[]> results;
        private final BlockingQueue<byte[]> incoming;



        public TestWorkerQueue(final int maxTasks, final BlockingQueue<byte[]> results, final BlockingQueue<byte[]> incoming)
        {
            super(maxTasks);
            this.results = results;
            this.incoming = incoming;
        }


        @Override
        public void start(final NewTaskCallback callback)
            throws QueueException
        {
            new Thread(new IncomingPoll(callback)).start();
        }


        @Override
        public void publish(String acknowledgeId, byte[] taskMessage, String targetQueue)
            throws QueueException
        {
            results.offer(taskMessage);
        }


        @Override
        public void rejectTask(final String taskId)
        {
        }


        @Override
        public void shutdownIncoming()
        {
        }


        @Override
        public void shutdown()
        {
        }


        @Override
        public WorkerQueueMetricsReporter getMetrics()
        {
            return Mockito.mock(WorkerQueueMetricsReporter.class);
        }


        @Override
        public HealthResult healthCheck()
        {
            return HealthResult.RESULT_HEALTHY;
        }


        private class IncomingPoll implements Runnable
        {
            private final NewTaskCallback callback;


            public IncomingPoll(final NewTaskCallback callback)
            {
                this.callback = callback;
            }


            @Override
            public void run()
            {
                try {
                    byte[] stuff = incoming.poll(5000, TimeUnit.MILLISECONDS);
                    callback.registerNewTask(JOB_ID, stuff);
                } catch (WorkerException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    private class TestWorkerJob
    {
        private String data = "test123";


        public TestWorkerJob() { }


        public TestWorkerJob(final String input)
        {
            this.data = input;
        }


        public String getData()
        {
            return data;
        }


        public void setData(final String data)
        {
            this.data = data;
        }
    }
}
