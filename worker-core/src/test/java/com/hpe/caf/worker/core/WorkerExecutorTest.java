/*
 * Copyright 2015-2023 Open Text.
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
import com.hpe.caf.api.worker.*;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.naming.ServicePath;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.naming.InvalidNameException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

public class WorkerExecutorTest
{
    @Test
    public void testExecuteTask()
        throws InvalidNameException, TaskRejectedException, InvalidTaskException, InterruptedException
    {
        Codec codec = new JsonCodec();
        ServicePath path = new ServicePath("/unitTest/test");
        WorkerCallback callback = mock(WorkerCallback.class);
        Worker worker = mock(Worker.class);
        WorkerResponse workerResponse = new WorkerResponse("testQueue", TaskStatus.NEW_TASK, new byte[0], "testType", 1, new byte[0]);
        Mockito.when(worker.doWork()).thenReturn(workerResponse);
        WorkerFactory factory = mock(WorkerFactory.class);
        Mockito.when(factory.getWorker(Mockito.any())).thenReturn(worker);
        WorkerThreadPool pool = WorkerThreadPool.create(5);
        Map<String, Object> headers = new HashMap<>();

        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, pool);
        TaskMessage tm = new TaskMessage("test", "test", 1, "test".getBytes(StandardCharsets.UTF_8), TaskStatus.NEW_TASK, new HashMap<>(), "testTo");
        executor.executeTask(tm, mock(TaskInformation.class), false, headers, codec);
        Mockito.verify(factory, Mockito.times(1)).getWorker(Mockito.any());
    }

    @Test
    public void testDefaultForwardTask()
        throws InvalidNameException, TaskRejectedException, InvalidTaskException
    {
        ServicePath path = new ServicePath("/unitTest/test");
        WorkerCallback callback = mock(WorkerCallback.class);
        Worker worker = mock(Worker.class);
        WorkerFactory factory = mock(WorkerFactory.class);
        Mockito.when(factory.getWorker(Mockito.any())).thenReturn(worker);
        WorkerThreadPool pool = mock(WorkerThreadPool.class);

        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, pool);
        TaskMessage tm = new TaskMessage("test", "test", 1, "test".getBytes(StandardCharsets.UTF_8), TaskStatus.NEW_TASK, new HashMap<>(), "testTo");
        TaskInformation taskInformation = mock(TaskInformation.class);
        executor.handleDivertedTask(tm, taskInformation, false, new HashMap<>(), null, null);
        Mockito.verify(callback, Mockito.times(1)).forward(taskInformation, "testTo", tm, new HashMap<>());
    }

    @Test
    public void testForwardDiscardTask()
        throws InvalidNameException, TaskRejectedException, InvalidTaskException
    {
        ServicePath path = new ServicePath("/unitTest/test");
        TaskMessage tm = new TaskMessage("test", "test", 1, "test".getBytes(StandardCharsets.UTF_8), TaskStatus.NEW_TASK, new HashMap<>(), "testTo");
        WorkerCallback callback = mock(WorkerCallback.class);
        Worker worker = mock(Worker.class);

        WorkerFactory factory = mock(WorkerFactory.class, withSettings().extraInterfaces(TaskMessageForwardingEvaluator.class));
        Mockito.doAnswer(new Answer()
        {
            public Object answer(InvocationOnMock invocation)
            {
                Object[] args = invocation.getArguments();

                TaskMessage invocation_tm = (TaskMessage) args[0];
                TaskInformation invocation_queueMessageId = (TaskInformation) args[1];
                Map<String, Object> invocation_headers = (Map<String, Object>) args[2];
                WorkerCallback invocation_callback = (WorkerCallback) args[3];

                invocation_callback.discard(invocation_queueMessageId);
                return null;
            }
        }).when((TaskMessageForwardingEvaluator) factory).determineForwardingAction(Mockito.any(TaskMessage.class), Mockito.any(TaskInformation.class), Mockito.anyMap(), Mockito.any(WorkerCallback.class));

        Mockito.when(factory.getWorker(Mockito.any())).thenReturn(worker);
        WorkerThreadPool pool = mock(WorkerThreadPool.class);

        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, pool);
        TaskInformation taskInformation =mock(TaskInformation.class);
        executor.handleDivertedTask(tm, taskInformation, false, new HashMap<>(), null, null);
        Mockito.verify((TaskMessageForwardingEvaluator) factory, Mockito.times(1)).determineForwardingAction(tm, taskInformation, new HashMap<>(), callback);
        Mockito.verify(callback, Mockito.times(1)).discard(taskInformation);
    }

    @Test(expectedExceptions = TaskRejectedException.class)
    public void testRejectTask()
        throws InvalidNameException, InvalidTaskException, TaskRejectedException
    {
        Codec codec = new JsonCodec();
        ServicePath path = new ServicePath("/unitTest/test");
        WorkerCallback callback = mock(WorkerCallback.class);
        WorkerFactory factory = mock(WorkerFactory.class);
        Mockito.when(factory.getWorker(Mockito.any())).thenThrow(
            TaskRejectedException.class);
        WorkerThreadPool pool = WorkerThreadPool.create(5);
        Map<String, Object> headers = new HashMap<>();

        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, pool);
        TaskMessage tm = new TaskMessage("test", "test", 1, "test".getBytes(StandardCharsets.UTF_8), TaskStatus.NEW_TASK, new HashMap<>(), "test");
        executor.executeTask(tm, mock(TaskInformation.class), false, headers, codec);
    }

    @Test
    public void testInvalidTask()
        throws InvalidNameException, InvalidTaskException, TaskRejectedException
    {
        Codec codec = new JsonCodec();
        ServicePath path = new ServicePath("/unitTest/test");
        String taskId = "testTask";
        String classifier = "classifier";
        int ver = 2;
        TaskInformation taskInformation = mock(TaskInformation.class);
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        String invalidQueue = "queue";
        WorkerCallback callback = mock(WorkerCallback.class);
        Answer<Void> a = invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            Assert.assertEquals(taskInformation, args[0]);
            Assert.assertEquals(invalidQueue, args[1]);
            TaskMessage tm = (TaskMessage) args[2];
            Assert.assertEquals(TaskStatus.INVALID_TASK, tm.getTaskStatus());
            Assert.assertEquals(taskId, tm.getTaskId());
            Assert.assertEquals(classifier, tm.getTaskClassifier());
            Assert.assertEquals(ver, tm.getTaskApiVersion());
            return null;
        };
        Mockito.doAnswer(a).when(callback).complete(Mockito.any(), Mockito.any(), Mockito.any());
        WorkerFactory factory = mock(WorkerFactory.class);
        Mockito.when(factory.getInvalidTaskQueue()).thenReturn(invalidQueue);
        Mockito.when(factory.getWorker(Mockito.any())).thenThrow(
            InvalidTaskException.class);
        Map<String, Object> headers = new HashMap<>();

        WorkerThreadPool pool = WorkerThreadPool.create(5);
        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, pool);

        TaskMessage tm = new TaskMessage(taskId, classifier, ver, data, TaskStatus.NEW_TASK, new HashMap<>(), "queue");
        executor.executeTask(tm, taskInformation, false, headers, codec);
        Mockito.verify(factory, Mockito.times(1)).getWorker(Mockito.any());
        Mockito.verify(callback, Mockito.times(1)).complete(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testEvenMoreInvalidTask()
        throws InvalidNameException, InvalidTaskException, TaskRejectedException
    {
        Codec codec = new JsonCodec();
        ServicePath path = new ServicePath("/unitTest/test");
        String taskId = "";
        String classifier = "";
        int ver = 0;
        TaskInformation taskInformation = mock(TaskInformation.class);
        byte[] data = new byte[]{};
        String invalidQueue = "queue";
        WorkerCallback callback = mock(WorkerCallback.class);
        Answer<Void> a = invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            Assert.assertEquals(taskInformation, args[0]);
            Assert.assertEquals(invalidQueue, args[1]);
            TaskMessage tm = (TaskMessage) args[2];
            Assert.assertEquals(TaskStatus.INVALID_TASK, tm.getTaskStatus());
            Assert.assertEquals(taskId, tm.getTaskId());
            Assert.assertEquals(classifier, tm.getTaskClassifier());
            Assert.assertEquals(ver, tm.getTaskApiVersion());
            return null;
        };
        Mockito.doAnswer(a).when(callback).complete(Mockito.any(), Mockito.any(), Mockito.any());
        WorkerFactory factory = mock(WorkerFactory.class);
        Mockito.when(factory.getInvalidTaskQueue()).thenReturn(invalidQueue);
        Mockito.when(factory.getWorker(Mockito.any())).thenThrow(
            InvalidTaskException.class);
        WorkerThreadPool pool = WorkerThreadPool.create(5);
        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, pool);
        Map<String, Object> headers = new HashMap<>();

        TaskMessage tm = new TaskMessage(taskId, classifier, ver, data, TaskStatus.NEW_TASK, new HashMap<>(), "queue");
        tm.setTaskId(null);
        tm.setTaskClassifier(null);
        tm.setTaskData(null);
        tm.setContext(null);

        executor.executeTask(tm, taskInformation, false, headers, codec);
        Mockito.verify(factory, Mockito.times(1)).getWorker(Mockito.any());
        Mockito.verify(callback, Mockito.times(1)).complete(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test(expectedExceptions = TaskRejectedException.class)
    public void testQueueFull()
        throws InvalidNameException, InvalidTaskException, TaskRejectedException
    {
        Codec codec = new JsonCodec();
        ServicePath path = new ServicePath("/unitTest/test");
        WorkerCallback callback = mock(WorkerCallback.class);
        Worker worker = mock(Worker.class);
        WorkerFactory factory = mock(WorkerFactory.class);
        Mockito.when(factory.getWorker(Mockito.any())).thenReturn(worker);
        WorkerThreadPool pool = mock(WorkerThreadPool.class);
        Mockito.doThrow(TaskRejectedException.class).when(pool).submitWorkerTask(Mockito.any(WorkerTaskImpl.class));
        Map<String, Object> headers = new HashMap<>();

        WorkerExecutor executor = new WorkerExecutor(path, callback, factory, pool);
        TaskMessage tm = new TaskMessage("test", "test", 1, "test".getBytes(StandardCharsets.UTF_8), TaskStatus.NEW_TASK, new HashMap<>(), "test");
        executor.executeTask(tm, mock(TaskInformation.class), false, headers, codec);
    }
}
