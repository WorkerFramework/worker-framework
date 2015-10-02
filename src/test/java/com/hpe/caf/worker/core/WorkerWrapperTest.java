package com.hpe.caf.worker.core;


import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.ServicePath;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskFailedException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.codec.JsonCodec;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.naming.InvalidNameException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class WorkerWrapperTest
{
    private static final String SUCCESS = "success";
    private static final byte[] SUCCESS_BYTES = SUCCESS.getBytes(StandardCharsets.UTF_8);
    private static final String QUEUE_OUT = "out";
    private static final String QUEUE_REDIRECT = "redirect";
    private static final String WORKER_NAME = "unitTest";
    private static final String REDIRECT_NAME = "newTask";
    private static final int WORKER_API_VER = 1;
    private static final String TASK_ID = "testTask";
    private static final String SERVICE_NAME = "/test/group";


    @Test
    public void testSuccess()
        throws WorkerException, InterruptedException, InvalidNameException, CodecException
    {
        Codec codec = new JsonCodec();
        Worker happyWorker = getWorker(new TestWorkerTask(), codec);
        String queueMsgId = "success";
        CountDownLatch latch = new CountDownLatch(1);
        TestCallback callback = new TestCallback(latch);
        TaskMessage m = new TaskMessage();
        m.setTaskId(TASK_ID);
        ServicePath path = new ServicePath(SERVICE_NAME);
        WorkerWrapper wrapper = new WorkerWrapper(m, queueMsgId, happyWorker, callback, path);
        Thread t = new Thread(wrapper);
        t.start();
        latch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(queueMsgId, callback.getQueueMsgId());
        Assert.assertEquals(TaskStatus.RESULT_SUCCESS, callback.getStatus());
        TestWorkerResult res = codec.deserialise(callback.getResultData(), TestWorkerResult.class);
        Assert.assertEquals(SUCCESS, res.getResultString());
        Assert.assertEquals(TASK_ID, callback.getTaskId());
        Assert.assertEquals(QUEUE_OUT, callback.getQueue());
        Assert.assertTrue(callback.getContext().containsKey(path.toString()));
        Assert.assertArrayEquals(SUCCESS_BYTES, callback.getContext().get(path.toString()));
    }


    @Test
    public void testNewTask()
        throws WorkerException, InterruptedException, InvalidNameException
    {
        Codec codec = new JsonCodec();
        Worker happyWorker = getRedirectWorker(new TestWorkerTask(), codec);
        String queueMsgId = "success";
        CountDownLatch latch = new CountDownLatch(1);
        TestCallback callback = new TestCallback(latch);
        TaskMessage m = new TaskMessage();
        m.setTaskId(TASK_ID);
        ServicePath path = new ServicePath(SERVICE_NAME);
        WorkerWrapper wrapper = new WorkerWrapper(m, queueMsgId, happyWorker, callback, path);
        Thread t = new Thread(wrapper);
        t.start();
        latch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(queueMsgId, callback.getQueueMsgId());
        Assert.assertEquals(TaskStatus.NEW_TASK, callback.getStatus());
        Assert.assertArrayEquals(SUCCESS_BYTES, callback.getResultData());
        Assert.assertEquals(TASK_ID, callback.getTaskId());
        Assert.assertEquals(REDIRECT_NAME, callback.getClassifier());
        Assert.assertEquals(QUEUE_REDIRECT, callback.getQueue());
    }


    @Test
    public void testException()
        throws WorkerException, InterruptedException, InvalidNameException, CodecException
    {
        Codec codec = new JsonCodec();
        Worker happyWorker = Mockito.spy(getWorker(new TestWorkerTask(), codec));
        Mockito.when(happyWorker.doWork()).thenAnswer(invocationOnMock -> {
            throw new TaskFailedException("whoops");
        });
        String queueMsgId = "exception";
        CountDownLatch latch = new CountDownLatch(1);
        TestCallback callback = new TestCallback(latch);
        TaskMessage m = new TaskMessage();
        ServicePath path = new ServicePath(SERVICE_NAME);
        Map<String, byte[]> contextMap = new HashMap<>();
        contextMap.put(path.toString(), SUCCESS_BYTES);
        m.setTaskId(TASK_ID);
        m.setContext(contextMap);
        WorkerWrapper wrapper = new WorkerWrapper(m, queueMsgId, happyWorker, callback, path);
        Thread t = new Thread(wrapper);
        t.start();
        latch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(queueMsgId, callback.getQueueMsgId());
        Assert.assertEquals(TaskStatus.RESULT_EXCEPTION, callback.getStatus());
        Assert.assertEquals(TASK_ID, callback.getTaskId());
        Assert.assertEquals(QUEUE_OUT, callback.getQueue());
        Assert.assertTrue(callback.getContext().containsKey(path.toString()));
        Assert.assertArrayEquals(SUCCESS_BYTES, callback.getContext().get(path.toString()));
        Class s = codec.deserialise(callback.getResultData(), Class.class);
        Assert.assertEquals(TaskFailedException.class.getName(), s.getName());
    }


    @Test
    public void testInterrupt()
        throws WorkerException, InterruptedException, InvalidNameException, CodecException
    {
        Codec codec = new JsonCodec();
        Worker happyWorker = Mockito.spy(getWorker(new TestWorkerTask(), codec));
        Mockito.when(happyWorker.doWork()).thenAnswer(invocationOnMock -> {
            throw new InterruptedException("interrupting!");
        });
        String queueMsgId = "interrupt";
        WorkerCallback callback = Mockito.mock(WorkerCallback.class);
        TaskMessage m = new TaskMessage();
        ServicePath path = new ServicePath(SERVICE_NAME);
        m.setTaskId(TASK_ID);
        WorkerWrapper wrapper = new WorkerWrapper(m, queueMsgId, happyWorker, callback, path);
        Thread t = new Thread(wrapper);
        t.start();
        Thread.sleep(1000);
        Mockito.verify(callback, Mockito.times(0)).complete(Mockito.any(), Mockito.any(), Mockito.any());
    }


    @Test
    public void testAbandon()
        throws InvalidTaskException, TaskRejectedException, InterruptedException, InvalidNameException
    {
        Codec codec = new JsonCodec();
        Worker happyWorker = Mockito.spy(getWorker(new TestWorkerTask(), codec));
        Mockito.when(happyWorker.doWork()).thenAnswer(invocationOnMock -> {
            throw new TaskRejectedException("bye!");
        });
        String queueMsgId = "abandon";
        CountDownLatch latch = new CountDownLatch(1);
        TestCallback callback = new TestCallback(latch);
        TaskMessage m = new TaskMessage();
        ServicePath path = new ServicePath(SERVICE_NAME);
        m.setTaskId(TASK_ID);
        WorkerWrapper wrapper = new WorkerWrapper(m, queueMsgId, happyWorker, callback, path);
        Thread t = new Thread(wrapper);
        t.start();
        latch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(queueMsgId, callback.getQueueMsgId());
    }


    private Worker getWorker(final TestWorkerTask task, final Codec codec)
        throws InvalidTaskException
    {
        return new Worker<TestWorkerTask, TestWorkerResult>(task, QUEUE_OUT, codec)
        {
            @Override
            public WorkerResponse doWork()
            {
                TestWorkerResult result = new TestWorkerResult();
                result.setResultString(SUCCESS);
                return createSuccessResult(result, SUCCESS_BYTES);
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
        };
    }


    private Worker getRedirectWorker(final TestWorkerTask task, final Codec codec)
        throws InvalidTaskException
    {
        return new Worker<TestWorkerTask, TestWorkerResult>(task, QUEUE_OUT, codec)
        {
            @Override
            public WorkerResponse doWork()
            {
                return createTaskSubmission(QUEUE_REDIRECT, SUCCESS_BYTES, REDIRECT_NAME, WORKER_API_VER);
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
        };
    }


    private final class TestCallback implements WorkerCallback
    {
        private String queueMsgId;
        private String taskId;
        private TaskStatus status;
        private byte[] resultData;
        private String queue;
        private String classifier;
        private Map<String, byte[]> context;
        private final CountDownLatch latch;


        public TestCallback(final CountDownLatch latch)
        {
            this.latch = Objects.requireNonNull(latch);
        }


        @Override
        public void complete(final String queueMsgId, final String queue, final TaskMessage tm)
        {
            this.queueMsgId = queueMsgId;
            this.status = tm.getTaskStatus();
            this.resultData = tm.getTaskData();
            this.taskId = tm.getTaskId();
            this.queue = queue;
            this.context = tm.getContext();
            this.classifier = tm.getTaskClassifier();
            latch.countDown();
        }


        @Override
        public void abandon(final String queueMsgId)
        {
            this.queueMsgId = queueMsgId;
            latch.countDown();
        }


        public String getClassifier()
        {
            return classifier;
        }


        public Map<String, byte[]> getContext()
        {
            return context;
        }


        public String getTaskId()
        {
            return taskId;
        }


        public String getQueueMsgId()
        {
            return queueMsgId;
        }


        public TaskStatus getStatus()
        {
            return status;
        }


        public byte[] getResultData()
        {
            return resultData;
        }


        public String getQueue()
        {
            return queue;
        }
    }
}
