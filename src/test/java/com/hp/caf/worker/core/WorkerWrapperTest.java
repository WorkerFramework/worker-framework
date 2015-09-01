package com.hp.caf.worker.core;


import com.hp.caf.api.ServicePath;
import com.hp.caf.api.worker.TaskMessage;
import com.hp.caf.api.worker.TaskStatus;
import com.hp.caf.api.worker.Worker;
import com.hp.caf.api.worker.WorkerException;
import com.hp.caf.api.worker.WorkerResponse;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.naming.InvalidNameException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


public class WorkerWrapperTest
{
    private static final byte[] SUCCESS_BYTES = "success".getBytes(StandardCharsets.UTF_8);
    private static final byte[] EXCEPTION_BYTES = "exception".getBytes(StandardCharsets.UTF_8);
    private static final String QUEUE_OUT = "out";
    private static final String QUEUE_REDIRECT = "redirect";
    private static final String WORKER_NAME = "unitTest";
    private static final String REDIRECT_NAME = "newTask";
    private static final int WORKER_API_VER = 1;
    private static final String TASK_ID = "testTask";
    private static final String SERVICE_NAME = "/test/group";


    @Test
    public void testSuccess()
        throws WorkerException, InterruptedException, InvalidNameException
    {
        Worker happyWorker = getWorker();
        String queueMsgId = "success";
        TestCallback callback = new TestCallback();
        TaskMessage m = new TaskMessage();
        m.setTaskId(TASK_ID);
        ServicePath path = new ServicePath(SERVICE_NAME);
        WorkerWrapper wrapper = new WorkerWrapper(m, queueMsgId, happyWorker, callback, path);
        Thread t = new Thread(wrapper);
        t.start();
        t.join(5000);
        Assert.assertEquals(queueMsgId, callback.getQueueMsgId());
        Assert.assertEquals(TaskStatus.RESULT_SUCCESS, callback.getStatus());
        Assert.assertArrayEquals(SUCCESS_BYTES, callback.getResultData());
        Assert.assertEquals(TASK_ID, callback.getTaskId());
        Assert.assertEquals(QUEUE_OUT, callback.getQueue());
        Assert.assertTrue(callback.getContext().containsKey(path.toString()));
        Assert.assertArrayEquals(SUCCESS_BYTES, callback.getContext().get(path.toString()));
    }


    @Test
    public void testNewTask()
        throws WorkerException, InterruptedException, InvalidNameException
    {
        Worker happyWorker = getRedirectWorker();
        String queueMsgId = "success";
        TestCallback callback = new TestCallback();
        TaskMessage m = new TaskMessage();
        m.setTaskId(TASK_ID);
        ServicePath path = new ServicePath(SERVICE_NAME);
        WorkerWrapper wrapper = new WorkerWrapper(m, queueMsgId, happyWorker, callback, path);
        Thread t = new Thread(wrapper);
        t.start();
        t.join(5000);
        Assert.assertEquals(queueMsgId, callback.getQueueMsgId());
        Assert.assertEquals(TaskStatus.NEW_TASK, callback.getStatus());
        Assert.assertArrayEquals(SUCCESS_BYTES, callback.getResultData());
        Assert.assertEquals(TASK_ID, callback.getTaskId());
        Assert.assertEquals(REDIRECT_NAME, callback.getClassifier());
        Assert.assertEquals(QUEUE_REDIRECT, callback.getQueue());
    }


    @Test
    public void testException()
        throws WorkerException, InterruptedException, InvalidNameException
    {
        Worker happyWorker = Mockito.spy(getWorker());
        Mockito.when(happyWorker.doWork()).thenThrow(WorkerException.class);
        String queueMsgId = "exception";
        TestCallback callback = new TestCallback();
        TaskMessage m = new TaskMessage();
        ServicePath path = new ServicePath(SERVICE_NAME);
        Map<String, byte[]> contextMap = new HashMap<>();
        contextMap.put(path.toString(), EXCEPTION_BYTES);
        m.setTaskId(TASK_ID);
        m.setContext(contextMap);
        WorkerWrapper wrapper = new WorkerWrapper(m, queueMsgId, happyWorker, callback, path);
        Thread t = new Thread(wrapper);
        t.start();
        t.join(5000);
        Assert.assertEquals(queueMsgId, callback.getQueueMsgId());
        Assert.assertEquals(TaskStatus.RESULT_EXCEPTION, callback.getStatus());
        Assert.assertEquals(EXCEPTION_BYTES, callback.getResultData());
        Assert.assertEquals(TASK_ID, callback.getTaskId());
        Assert.assertEquals(QUEUE_OUT, callback.getQueue());
        Assert.assertTrue(callback.getContext().containsKey(path.toString()));
        Assert.assertArrayEquals(EXCEPTION_BYTES, callback.getContext().get(path.toString()));
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


    private Worker getRedirectWorker()
    {
        return new Worker(QUEUE_OUT)
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


            @Override
            protected byte[] getGeneralFailureData()
            {
                return EXCEPTION_BYTES;
            }
        };
    }


    private final class TestCallback implements CompleteTaskCallback
    {
        private String queueMsgId;
        private String taskId;
        private TaskStatus status;
        private byte[] resultData;
        private String queue;
        private String classifier;
        private Map<String, byte[]> context;


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
