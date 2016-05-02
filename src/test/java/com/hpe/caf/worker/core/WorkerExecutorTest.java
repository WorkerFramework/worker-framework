package com.hpe.caf.worker.core;

import com.hpe.caf.api.worker.*;
import com.hpe.caf.naming.ServicePath;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.naming.InvalidNameException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.withSettings;

public class WorkerExecutorTest
{
    @Test
    public void testExecuteTask()
        throws InvalidNameException, TaskRejectedException, InvalidTaskException
    {
        ServicePath path = new ServicePath("/unitTest/test");
        WorkerCallback callback = Mockito.mock(WorkerCallback.class);
        Worker worker = Mockito.mock(Worker.class);
        WorkerFactory factory = Mockito.mock(WorkerFactory.class);
        Mockito.when(factory.getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(worker);
        WorkerThreadPool pool = WorkerThreadPool.create(5);

        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, pool);
        TaskMessage tm = new TaskMessage("test", "test", 1, "test".getBytes(StandardCharsets.UTF_8), TaskStatus.NEW_TASK, new HashMap<>(), "testTo");
        executor.executeTask(tm, "test");
        Mockito.verify(factory, Mockito.times(1)).getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }


    @Test
    public void testDefaultForwardTask()
            throws InvalidNameException, TaskRejectedException, InvalidTaskException
    {
        ServicePath path = new ServicePath("/unitTest/test");
        WorkerCallback callback = Mockito.mock(WorkerCallback.class);
        Worker worker = Mockito.mock(Worker.class);
        WorkerFactory factory = Mockito.mock(WorkerFactory.class);
        Mockito.when(factory.getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(worker);
        WorkerThreadPool pool = Mockito.mock(WorkerThreadPool.class);

        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, pool);
        TaskMessage tm = new TaskMessage("test", "test", 1, "test".getBytes(StandardCharsets.UTF_8), TaskStatus.NEW_TASK, new HashMap<>(), "testTo");
        executor.forwardTask(tm, "testMsgId", new HashMap<>());
        Mockito.verify(callback, Mockito.times(1)).forward("testMsgId", "testTo", tm, new HashMap<>());
    }


    @Test
    public void testForwardDiscardTask()
            throws InvalidNameException, TaskRejectedException, InvalidTaskException
    {
        ServicePath path = new ServicePath("/unitTest/test");
        TaskMessage tm = new TaskMessage("test", "test", 1, "test".getBytes(StandardCharsets.UTF_8), TaskStatus.NEW_TASK, new HashMap<>(), "testTo");
        WorkerCallback callback = Mockito.mock(WorkerCallback.class);
        Worker worker = Mockito.mock(Worker.class);

        WorkerFactory factory = Mockito.mock(WorkerFactory.class, withSettings().extraInterfaces(TaskMessageForwardingEvaluator.class));
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();

                TaskMessage invocation_tm = (TaskMessage) args[0];
                String invocation_queueMessageId = (String) args[1];
                Map<String, Object> invocation_headers = (Map<String, Object>)args[2];
                WorkerCallback invocation_callback = (WorkerCallback) args[3];

                invocation_callback.discard(invocation_queueMessageId);
                return null;
            }}).when((TaskMessageForwardingEvaluator)factory).determineForwardingAction(Mockito.any(TaskMessage.class), Mockito.anyString(), Mockito.anyMap(), Mockito.any(WorkerCallback.class));

        Mockito.when(factory.getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(worker);
        WorkerThreadPool pool = Mockito.mock(WorkerThreadPool.class);

        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, pool);
        executor.forwardTask(tm, "testMsgId", new HashMap<>());
        Mockito.verify((TaskMessageForwardingEvaluator)factory, Mockito.times(1)).determineForwardingAction(tm, "testMsgId", new HashMap<>(), callback);
        Mockito.verify(callback, Mockito.times(1)).discard("testMsgId");
    }


    @Test(expected = TaskRejectedException.class)
    public void testRejectTask()
        throws InvalidNameException, InvalidTaskException, TaskRejectedException
    {
        ServicePath path = new ServicePath("/unitTest/test");
        WorkerCallback callback = Mockito.mock(WorkerCallback.class);
        WorkerFactory factory = Mockito.mock(WorkerFactory.class);
        Mockito.when(factory.getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(
            TaskRejectedException.class);
        WorkerThreadPool pool = WorkerThreadPool.create(5);

        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, pool);
        TaskMessage tm = new TaskMessage("test", "test", 1, "test".getBytes(StandardCharsets.UTF_8), TaskStatus.NEW_TASK, new HashMap<>(), "test");
        executor.executeTask(tm, "test");
    }


    @Test
    public void testInvalidTask()
        throws InvalidNameException, InvalidTaskException, TaskRejectedException
    {
        ServicePath path = new ServicePath("/unitTest/test");
        String taskId = "testTask";
        String classifier = "classifier";
        int ver = 2;
        String msgId = "testMsg";
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        String invalidQueue = "queue";
        WorkerCallback callback = Mockito.mock(WorkerCallback.class);
        Answer<Void> a = invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            Assert.assertEquals(msgId, args[0]);
            Assert.assertEquals(invalidQueue, args[1]);
            TaskMessage tm = (TaskMessage) args[2];
            Assert.assertEquals(TaskStatus.INVALID_TASK, tm.getTaskStatus());
            Assert.assertEquals(taskId, tm.getTaskId());
            Assert.assertEquals(classifier, tm.getTaskClassifier());
            Assert.assertEquals(ver, tm.getTaskApiVersion());
            return null;
        };
        Mockito.doAnswer(a).when(callback).complete(Mockito.any(), Mockito.any(), Mockito.any());
        WorkerFactory factory = Mockito.mock(WorkerFactory.class);
        Mockito.when(factory.getInvalidTaskQueue()).thenReturn(invalidQueue);
        Mockito.when(factory.getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(
            InvalidTaskException.class);
        WorkerThreadPool pool = WorkerThreadPool.create(5);
        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, pool);

        TaskMessage tm = new TaskMessage(taskId, classifier, ver, data, TaskStatus.NEW_TASK, new HashMap<>(), "queue");
        executor.executeTask(tm, msgId);
        Mockito.verify(factory, Mockito.times(1)).getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(callback, Mockito.times(1)).complete(Mockito.any(), Mockito.any(), Mockito.any());
    }


    @Test
    public void testEvenMoreInvalidTask()
        throws InvalidNameException, InvalidTaskException, TaskRejectedException
    {
        ServicePath path = new ServicePath("/unitTest/test");
        String taskId = "";
        String classifier = "";
        int ver = 0;
        String msgId = "testMsg";
        byte[] data = new byte[] {};
        String invalidQueue = "queue";
        WorkerCallback callback = Mockito.mock(WorkerCallback.class);
        Answer<Void> a = invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            Assert.assertEquals(msgId, args[0]);
            Assert.assertEquals(invalidQueue, args[1]);
            TaskMessage tm = (TaskMessage) args[2];
            Assert.assertEquals(TaskStatus.INVALID_TASK, tm.getTaskStatus());
            Assert.assertEquals(taskId, tm.getTaskId());
            Assert.assertEquals(classifier, tm.getTaskClassifier());
            Assert.assertEquals(ver, tm.getTaskApiVersion());
            return null;
        };
        Mockito.doAnswer(a).when(callback).complete(Mockito.any(), Mockito.any(), Mockito.any());
        WorkerFactory factory = Mockito.mock(WorkerFactory.class);
        Mockito.when(factory.getInvalidTaskQueue()).thenReturn(invalidQueue);
        Mockito.when(factory.getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(
            InvalidTaskException.class);
        WorkerThreadPool pool = WorkerThreadPool.create(5);
        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, pool);

        TaskMessage tm = new TaskMessage(taskId, classifier, ver, data, TaskStatus.NEW_TASK, new HashMap<>(), "queue");
        tm.setTaskId(null);
        tm.setTaskClassifier(null);
        tm.setTaskData(null);
        tm.setContext(null);

        executor.executeTask(tm, msgId);
        Mockito.verify(factory, Mockito.times(1)).getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(callback, Mockito.times(1)).complete(Mockito.any(), Mockito.any(), Mockito.any());
    }


    @Test(expected = TaskRejectedException.class)
    public void testQueueFull()
        throws InvalidNameException, InvalidTaskException, TaskRejectedException
    {
        ServicePath path = new ServicePath("/unitTest/test");
        WorkerCallback callback = Mockito.mock(WorkerCallback.class);
        Worker worker = Mockito.mock(Worker.class);
        WorkerFactory factory = Mockito.mock(WorkerFactory.class);
        Mockito.when(factory.getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(worker);
        WorkerThreadPool pool = Mockito.mock(WorkerThreadPool.class);
        Mockito.doThrow(TaskRejectedException.class).when(pool).submitWorkerTask(Mockito.any(WorkerTaskImpl.class));

        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, pool);
        TaskMessage tm = new TaskMessage("test", "test", 1, "test".getBytes(StandardCharsets.UTF_8), TaskStatus.NEW_TASK, new HashMap<>(), "test");
        executor.executeTask(tm, "test");
    }
}
