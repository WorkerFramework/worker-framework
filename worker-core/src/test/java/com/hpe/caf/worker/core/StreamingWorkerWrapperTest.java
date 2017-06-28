/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.worker.core;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.naming.ServicePath;
import com.hpe.caf.worker.AbstractWorker;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.mockito.Mockito;
import org.testng.internal.junit.ArrayAsserts;

import javax.naming.InvalidNameException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StreamingWorkerWrapperTest
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
    private static final Integer PRIORITY = 2;


    @Test
    public void testSuccess()
        throws WorkerException, InterruptedException, InvalidNameException, CodecException
    {
        Codec codec = new JsonCodec();
        WorkerFactory happyWorkerFactory = Mockito.mock(WorkerFactory.class);
        Worker happyWorker = getWorker(new TestWorkerTask(), codec);
        Mockito.when(happyWorkerFactory.getWorker(Mockito.any())).thenReturn(happyWorker);
        MessagePriorityManager priorityManager = Mockito.mock(MessagePriorityManager.class);
        Mockito.when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(PRIORITY);
        String queueMsgId = "success";
        CountDownLatch latch = new CountDownLatch(1);
        TestCallback callback = new TestCallback(latch);
        TaskMessage m = new TaskMessage();
        m.setTaskId(TASK_ID);
        ServicePath path = new ServicePath(SERVICE_NAME);
        WorkerTaskImpl workerTask = new WorkerTaskImpl(path, callback, happyWorkerFactory, queueMsgId, m, false, priorityManager);
        StreamingWorkerWrapper wrapper = new StreamingWorkerWrapper(workerTask);
        Thread t = new Thread(wrapper);
        t.start();
        latch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(queueMsgId, callback.getQueueMsgId());
        Assert.assertEquals(TaskStatus.RESULT_SUCCESS, callback.getStatus());
        TestWorkerResult res = codec.deserialise(callback.getResultData(), TestWorkerResult.class);
        Assert.assertEquals(SUCCESS, res.getResultString());
        Assert.assertEquals(PRIORITY, callback.getPriority());
        Assert.assertEquals(TASK_ID, callback.getTaskId());
        Assert.assertEquals(QUEUE_OUT, callback.getQueue());
        Assert.assertTrue(callback.getContext().containsKey(path.toString()));
        ArrayAsserts.assertArrayEquals(SUCCESS_BYTES, callback.getContext().get(path.toString()));
    }


    @Test
    public void testNewTask()
        throws WorkerException, InterruptedException, InvalidNameException
    {
        Codec codec = new JsonCodec();
        WorkerFactory happyWorkerFactory = Mockito.mock(WorkerFactory.class);
        Worker happyWorker = getRedirectWorker(new TestWorkerTask(), codec);
        Mockito.when(happyWorkerFactory.getWorker(Mockito.any())).thenReturn(happyWorker);
        MessagePriorityManager priorityManager = Mockito.mock(MessagePriorityManager.class);
        Mockito.when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(PRIORITY);

        String queueMsgId = "success";
        CountDownLatch latch = new CountDownLatch(1);
        TestCallback callback = new TestCallback(latch);
        TaskMessage m = new TaskMessage();
        m.setTaskId(TASK_ID);
        ServicePath path = new ServicePath(SERVICE_NAME);
        WorkerTaskImpl workerTask = new WorkerTaskImpl(path, callback, happyWorkerFactory, queueMsgId, m, false, priorityManager);
        StreamingWorkerWrapper wrapper = new StreamingWorkerWrapper(workerTask);
        Thread t = new Thread(wrapper);
        t.start();
        latch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(queueMsgId, callback.getQueueMsgId());
        Assert.assertEquals(TaskStatus.NEW_TASK, callback.getStatus());
        ArrayAsserts.assertArrayEquals(SUCCESS_BYTES, callback.getResultData());
        Assert.assertEquals(TASK_ID, callback.getTaskId());
        Assert.assertEquals(REDIRECT_NAME, callback.getClassifier());
        Assert.assertEquals(QUEUE_REDIRECT, callback.getQueue());
    }


    @Test
    public void testException()
        throws WorkerException, InterruptedException, InvalidNameException, CodecException
    {
        Codec codec = new JsonCodec();
        WorkerFactory happyWorkerFactory = Mockito.mock(WorkerFactory.class);
        Worker happyWorker = Mockito.spy(getWorker(new TestWorkerTask(), codec));
        Mockito.when(happyWorkerFactory.getWorker(Mockito.any())).thenReturn(happyWorker);
        Mockito.when(happyWorker.doWork()).thenAnswer(invocationOnMock -> {
            throw new TaskFailedException("whoops");
        });
        MessagePriorityManager priorityManager = Mockito.mock(MessagePriorityManager.class);
        Mockito.when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(PRIORITY);
        String queueMsgId = "exception";
        CountDownLatch latch = new CountDownLatch(1);
        TestCallback callback = new TestCallback(latch);
        TaskMessage m = new TaskMessage();
        ServicePath path = new ServicePath(SERVICE_NAME);
        Map<String, byte[]> contextMap = new HashMap<>();
        contextMap.put(path.toString(), SUCCESS_BYTES);
        m.setTaskId(TASK_ID);
        m.setContext(contextMap);
        WorkerTaskImpl workerTask = new WorkerTaskImpl(path, callback, happyWorkerFactory, queueMsgId, m, false, priorityManager);
        StreamingWorkerWrapper wrapper = new StreamingWorkerWrapper(workerTask);
        Thread t = new Thread(wrapper);
        t.start();
        latch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(queueMsgId, callback.getQueueMsgId());
        Assert.assertEquals(TaskStatus.RESULT_EXCEPTION, callback.getStatus());
        Assert.assertEquals(TASK_ID, callback.getTaskId());
        Assert.assertEquals(QUEUE_OUT, callback.getQueue());
        Assert.assertTrue(callback.getContext().containsKey(path.toString()));
        ArrayAsserts.assertArrayEquals(SUCCESS_BYTES, callback.getContext().get(path.toString()));
        String s = codec.deserialise(callback.getResultData(), String.class);
        Assert.assertEquals(true, s.contains("class com.hpe.caf.api.worker.TaskFailedException whoops"));
    }


    @Test
    public void testInterrupt()
        throws WorkerException, InterruptedException, InvalidNameException, CodecException
    {
        Codec codec = new JsonCodec();
        WorkerFactory happyWorkerFactory = Mockito.mock(WorkerFactory.class);
        Worker happyWorker = Mockito.spy(getWorker(new TestWorkerTask(), codec));
        Mockito.when(happyWorkerFactory.getWorker(Mockito.any())).thenReturn(happyWorker);
        Mockito.when(happyWorker.doWork()).thenAnswer(invocationOnMock -> {
            throw new InterruptedException("interrupting!");
        });
        MessagePriorityManager priorityManager = Mockito.mock(MessagePriorityManager.class);
        Mockito.when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(PRIORITY);
        String queueMsgId = "interrupt";
        WorkerCallback callback = Mockito.mock(WorkerCallback.class);
        TaskMessage m = new TaskMessage();
        ServicePath path = new ServicePath(SERVICE_NAME);
        m.setTaskId(TASK_ID);
        WorkerTaskImpl workerTask = new WorkerTaskImpl(path, callback, happyWorkerFactory, queueMsgId, m, false, priorityManager);
        StreamingWorkerWrapper wrapper = new StreamingWorkerWrapper(workerTask);
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
        WorkerFactory happyWorkerFactory = Mockito.mock(WorkerFactory.class);
        Worker happyWorker = Mockito.spy(getWorker(new TestWorkerTask(), codec));
        Mockito.when(happyWorkerFactory.getWorker(Mockito.any())).thenReturn(happyWorker);
        Mockito.when(happyWorker.doWork()).thenAnswer(invocationOnMock -> {
            throw new TaskRejectedException("bye!");
        });
        MessagePriorityManager priorityManager = Mockito.mock(MessagePriorityManager.class);
        Mockito.when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(PRIORITY);
        String queueMsgId = "abandon";
        CountDownLatch latch = new CountDownLatch(1);
        TestCallback callback = new TestCallback(latch);
        TaskMessage m = new TaskMessage();
        ServicePath path = new ServicePath(SERVICE_NAME);
        m.setTaskId(TASK_ID);
        WorkerTaskImpl workerTask = new WorkerTaskImpl(path, callback, happyWorkerFactory, queueMsgId, m, false, priorityManager);
        StreamingWorkerWrapper wrapper = new StreamingWorkerWrapper(workerTask);
        Thread t = new Thread(wrapper);
        t.start();
        latch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(queueMsgId, callback.getQueueMsgId());
    }


    private Worker getWorker(final TestWorkerTask task, final Codec codec)
        throws InvalidTaskException
    {
        return new AbstractWorker<TestWorkerTask, TestWorkerResult>(task, QUEUE_OUT, codec)
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
        return new AbstractWorker<TestWorkerTask, TestWorkerResult>(task, QUEUE_OUT, codec)
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
        private Integer priority;
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
            this.priority = tm.getPriority();
            latch.countDown();
        }


        @Override
        public void abandon(final String queueMsgId, final Exception exception)
        {
            this.queueMsgId = queueMsgId;
            latch.countDown();
        }


        @Override
        public void forward(String queueMsgId, String queue, TaskMessage forwardedMessage, Map<String, Object> headers) {
            this.queueMsgId = queueMsgId;
            latch.countDown();
        }


        @Override
        public void discard(String queueMsgId) {
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

        public Integer getPriority()
        {
            return priority;
        }
    }
}
