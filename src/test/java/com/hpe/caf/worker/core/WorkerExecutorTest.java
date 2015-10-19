package com.hpe.caf.worker.core;


import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.naming.ServicePath;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import javax.naming.InvalidNameException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;


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
        Mockito.when(factory.getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(worker);
        Map<String, Future<?>> taskMap = new HashMap<>();
        BlockingQueue<Runnable> queue = Mockito.mock(BlockingQueue.class);
        Mockito.when(queue.size()).thenReturn(0);
        ThreadPoolExecutor pool = Mockito.mock(ThreadPoolExecutor.class);
        Mockito.when(pool.getCorePoolSize()).thenReturn(10);
        Mockito.when(pool.getQueue()).thenReturn(queue);
        Future<?> future = Mockito.mock(Future.class);
        Mockito.when(pool.submit(Mockito.any(Runnable.class))).then(invocationOnMock -> future);

        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, taskMap, pool);
        TaskMessage tm = new TaskMessage("test", "test", 1, "test".getBytes(StandardCharsets.UTF_8), TaskStatus.NEW_TASK, new HashMap<>());
        executor.executeTask(tm, "test");
        Mockito.verify(factory, Mockito.times(1)).getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(pool, Mockito.times(1)).submit(Mockito.any(Runnable.class));
    }


    @Test(expected = TaskRejectedException.class)
    public void testRejectTask()
        throws InvalidNameException, InvalidTaskException, TaskRejectedException
    {
        ServicePath path = new ServicePath("/unitTest/test");
        WorkerCallback callback = Mockito.mock(WorkerCallback.class);
        WorkerFactory factory = Mockito.mock(WorkerFactory.class);
        Mockito.when(factory.getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(
            TaskRejectedException.class);
        Map<String, Future<?>> taskMap = new HashMap<>();
        BlockingQueue<Runnable> queue = Mockito.mock(BlockingQueue.class);
        Mockito.when(queue.size()).thenReturn(0);
        ThreadPoolExecutor pool = Mockito.mock(ThreadPoolExecutor.class);
        Mockito.when(pool.getCorePoolSize()).thenReturn(10);
        Mockito.when(pool.getQueue()).thenReturn(queue);
        Future<?> future = Mockito.mock(Future.class);
        Mockito.when(pool.submit(Mockito.any(Runnable.class))).then(invocationOnMock -> future);

        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, taskMap, pool);
        TaskMessage tm = new TaskMessage("test", "test", 1, "test".getBytes(StandardCharsets.UTF_8), TaskStatus.NEW_TASK, new HashMap<>());
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
        Mockito.when(factory.getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(
            InvalidTaskException.class);
        Map<String, Future<?>> taskMap = new HashMap<>();
        BlockingQueue<Runnable> queue = Mockito.mock(BlockingQueue.class);
        Mockito.when(queue.size()).thenReturn(0);
        ThreadPoolExecutor pool = Mockito.mock(ThreadPoolExecutor.class);
        Mockito.when(pool.getCorePoolSize()).thenReturn(10);
        Mockito.when(pool.getQueue()).thenReturn(queue);
        Future<?> future = Mockito.mock(Future.class);
        Mockito.when(pool.submit(Mockito.any(Runnable.class))).then(invocationOnMock -> future);
        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, taskMap, pool);

        TaskMessage tm = new TaskMessage(taskId, classifier, ver, data, TaskStatus.NEW_TASK, new HashMap<>());
        executor.executeTask(tm, msgId);
        Mockito.verify(factory, Mockito.times(1)).getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(pool, Mockito.times(0)).submit(Mockito.any(Runnable.class));
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
        Mockito.when(factory.getWorker(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(worker);
        Map<String, Future<?>> taskMap = new HashMap<>();
        BlockingQueue<Runnable> queue = Mockito.mock(BlockingQueue.class);
        Mockito.when(queue.size()).thenReturn(20);
        ThreadPoolExecutor pool = Mockito.mock(ThreadPoolExecutor.class);
        Mockito.when(pool.getCorePoolSize()).thenReturn(1);
        Mockito.when(pool.getQueue()).thenReturn(queue);
        Future<?> future = Mockito.mock(Future.class);
        Mockito.when(pool.submit(Mockito.any(Runnable.class))).then(invocationOnMock -> future);

        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, taskMap, pool);
        TaskMessage tm = new TaskMessage("test", "test", 1, "test".getBytes(StandardCharsets.UTF_8), TaskStatus.NEW_TASK, new HashMap<>());
        executor.executeTask(tm, "test");
    }
}
