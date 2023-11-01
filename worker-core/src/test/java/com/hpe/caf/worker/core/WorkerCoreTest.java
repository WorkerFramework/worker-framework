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

import com.codahale.metrics.health.HealthCheckRegistry;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.naming.ServicePath;
import com.hpe.caf.worker.AbstractWorker;
import com.hpe.caf.worker.tracking.report.TrackingReportStatus;
import com.hpe.caf.worker.tracking.report.TrackingReportTask;
import com.hpe.caf.worker.tracking.report.TrackingReportConstants;
import com.hpe.caf.api.worker.QueueTaskMessage;

import java.io.File;
import java.net.MalformedURLException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.mockito.Mockito;
import org.testng.internal.junit.ArrayAsserts;

import javax.naming.InvalidNameException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkerCoreTest
{
    private static final Logger Logger = LoggerFactory.getLogger(WorkerCoreTest.class);
    
    private static final String SUCCESS = "success";
    private static final String FAILURE = "failure";
    private static final String WORKER_NAME = "testWorker";
    private static final int WORKER_API_VER = 1;
    private static final String QUEUE_IN = "inQueue";
    private static final String QUEUE_OUT = "outQueue";
    private static final String QUEUE_PAUSED = "pausedQueue";
    private static final String SERVICE_PATH = "/test/group";
    private static final int PRIORITY = 2;

    private TaskInformation taskInformation;

    @BeforeMethod
    private void before() {
        taskInformation = getMockTaskInformation("test1");
    }

    /**
     * Send a message all the way through WorkerCore and verify the result output message *
     */
    @Test
    public void testWorkerCore()
        throws CodecException, InterruptedException, WorkerException, ConfigurationException, QueueException, InvalidNameException
    {
        BlockingQueue<byte[]> q = new LinkedBlockingQueue<>();
        Codec codec = new JsonCodec();
        WorkerThreadPool wtp = WorkerThreadPool.create(5);
        ConfigurationSource config = Mockito.mock(ConfigurationSource.class);
        ServicePath path = new ServicePath(SERVICE_PATH);
        TestWorkerTask task = new TestWorkerTask();
        TestWorkerQueue queue = new TestWorkerQueueProvider(q).getWorkerQueue(config, 50);
        MessagePriorityManager priorityManager = Mockito.mock(MessagePriorityManager.class);
        Mockito.when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(PRIORITY);
        HealthCheckRegistry healthCheckRegistry = Mockito.mock(HealthCheckRegistry.class);
        TransientHealthCheck transientHealthCheck = Mockito.mock(TransientHealthCheck.class);

        WorkerCore core = new WorkerCore(codec, wtp, queue, priorityManager, getWorkerFactory(task, codec), path, healthCheckRegistry, transientHealthCheck);
        core.start();
        // at this point, the queue should hand off the task to the app, the app should get a worker from the mocked WorkerFactory,
        // and the Worker itself is a mock wrapped in a WorkerWrapper, which should return success and the appropriate result data
        byte[] stuff = codec.serialise(getTaskMessage(task, codec, WORKER_NAME));
        queue.submitTask(taskInformation, stuff);
        // the worker's task result should eventually be passed back to our dummy WorkerQueue and onto our blocking queue
        byte[] result = q.poll(5000, TimeUnit.MILLISECONDS);
        // if the result didn't get back to us, then result will be null
        Assert.assertNotNull(result);
        // deserialise and verify result data
        TaskMessage taskMessage = codec.deserialise(result, TaskMessage.class);
        Assert.assertEquals(TaskStatus.RESULT_SUCCESS, taskMessage.getTaskStatus());
        Assert.assertEquals(WORKER_NAME, taskMessage.getTaskClassifier());
        Assert.assertEquals(WORKER_API_VER, taskMessage.getTaskApiVersion());
        TestWorkerResult workerResult = codec.deserialise(taskMessage.getTaskData(), TestWorkerResult.class);
        Assert.assertEquals(SUCCESS, workerResult.getResultString());
        Assert.assertTrue(taskMessage.getContext().containsKey(path.toString()));
        ArrayAsserts.assertArrayEquals(SUCCESS.getBytes(StandardCharsets.UTF_8), taskMessage.getContext().get(path.toString()));
    }
    
    @Test
    public void testWorkerCoreWithTaskDataAsObject()
        throws CodecException, InterruptedException, WorkerException, QueueException, InvalidNameException
    {
        //prepare
        final BlockingQueue<byte[]> q = new LinkedBlockingQueue<>();
        final Codec codec = new JsonCodec();
        final WorkerThreadPool wtp = WorkerThreadPool.create(5);
        final ConfigurationSource config = mock(ConfigurationSource.class);
        final ServicePath path = new ServicePath(SERVICE_PATH);
        final TestWorkerTask task = new TestWorkerTask();
        final TestWorkerQueue queue = new TestWorkerQueueProvider(q).getWorkerQueue(config, 50);
        final MessagePriorityManager priorityManager = mock(MessagePriorityManager.class);
        when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(PRIORITY);
        final HealthCheckRegistry healthCheckRegistry = mock(HealthCheckRegistry.class);
        final TransientHealthCheck transientHealthCheck = mock(TransientHealthCheck.class);

        final WorkerCore core = new WorkerCore(codec, wtp, queue, priorityManager, getWorkerFactory(task, codec), path, healthCheckRegistry, transientHealthCheck);
        core.start();
        // at this point, the queue should hand off the task to the app, the app should get a worker from the mocked WorkerFactory,
        // and the Worker itself is a mock wrapped in a WorkerWrapper, which should return success and the appropriate result data
        final byte[] stuff = codec.serialise(getQueueTaskMessage(task, WORKER_NAME));
        
        //act
        queue.submitTask(taskInformation, stuff);
        // the worker's task result should eventually be passed back to our dummy WorkerQueue and onto our blocking queue
        final byte[] result = q.poll(5000, TimeUnit.MILLISECONDS);
        // if the result didn't get back to us, then result will be null
        Assert.assertNotNull(result);
        // deserialise and verify result data
        final TaskMessage taskMessage = codec.deserialise(result, TaskMessage.class);
        
        //assert
        Assert.assertEquals(TaskStatus.RESULT_SUCCESS, taskMessage.getTaskStatus());
        Assert.assertEquals(WORKER_NAME, taskMessage.getTaskClassifier());
        Assert.assertEquals(WORKER_API_VER, taskMessage.getTaskApiVersion());
        final TestWorkerResult workerResult = codec.deserialise(taskMessage.getTaskData(), TestWorkerResult.class);
        Assert.assertEquals(SUCCESS, workerResult.getResultString());
        Assert.assertTrue(taskMessage.getContext().containsKey(path.toString()));
        ArrayAsserts.assertArrayEquals(SUCCESS.getBytes(StandardCharsets.UTF_8), taskMessage.getContext().get(path.toString()));
    }

    /**
     * Send a message with tracking info *
     */
    @Test
    public void testWorkerCoreWithTracking()
        throws CodecException, InterruptedException, WorkerException, ConfigurationException, QueueException, InvalidNameException
    {
        final BlockingQueue<byte[]> q = new LinkedBlockingQueue<>();
        final Codec codec = new JsonCodec();
        final WorkerThreadPool wtp = WorkerThreadPool.create(5);
        final ConfigurationSource config = Mockito.mock(ConfigurationSource.class);
        final ServicePath path = new ServicePath(SERVICE_PATH);
        final TestWorkerTask task = new TestWorkerTask();
        final TestWorkerQueue queue = new TestWorkerQueueProvider(q).getWorkerQueue(config, 50);
        final MessagePriorityManager priorityManager = Mockito.mock(MessagePriorityManager.class);
        Mockito.when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(PRIORITY);
        final HealthCheckRegistry healthCheckRegistry = Mockito.mock(HealthCheckRegistry.class);
        final TransientHealthCheck transientHealthCheck = Mockito.mock(TransientHealthCheck.class);

        final WorkerCore core = new WorkerCore(codec, wtp, queue, priorityManager, getWorkerFactory(task, codec), path, healthCheckRegistry, transientHealthCheck);
        core.start();
        // at this point, the queue should hand off the task to the app, the app should get a worker from the mocked WorkerFactory,
        // and the Worker itself is a mock wrapped in a WorkerWrapper, which should return success and the appropriate result data
        final TrackingInfo tracking = new TrackingInfo("J23.1.2", new Date(), 0, "http://thehost:1234/job-service/v1/jobs/23/status", "trackingQueue", "trackTo");
        final byte[] stuff = codec.serialise(getTaskMessage(task, codec, WORKER_NAME, tracking));
        queue.submitTask(taskInformation, stuff);

        // Two results expected back. One for the report progress update and another for the message completion.
        //
        // Verify result for the report update call.
        final byte[] rutResult = q.poll(5000, TimeUnit.MILLISECONDS);
        // if the result didn't get back to us, then rutResult will be null
        Assert.assertNotNull(rutResult);
        // deserialise and verify rutResult data
        final TaskMessage rutTaskMessage = codec.deserialise(rutResult, TaskMessage.class);
        Assert.assertEquals(TaskStatus.NEW_TASK, rutTaskMessage.getTaskStatus());
        Assert.assertEquals(TrackingReportConstants.TRACKING_REPORT_TASK_NAME, rutTaskMessage.getTaskClassifier());
        Assert.assertEquals(TrackingReportConstants.TRACKING_REPORT_TASK_API_VER, rutTaskMessage.getTaskApiVersion());
        final TrackingReportTask rutWorkerResult = codec.deserialise(rutTaskMessage.getTaskData(), TrackingReportTask.class);
        Assert.assertEquals("J23.1.2", rutWorkerResult.trackingReports.get(0).jobTaskId);
        Assert.assertEquals(TrackingReportStatus.Progress, rutWorkerResult.trackingReports.get(0).status);
        // Verify result for message completion.
        final byte[] msgCompletionResult = q.poll(5000, TimeUnit.MILLISECONDS);
        // if the result didn't get back to us, then result will be null
        Assert.assertNotNull(msgCompletionResult);
        // deserialise and verify msgCompletionResult data
        final TaskMessage msgCompletionTaskMessage = codec.deserialise(msgCompletionResult, TaskMessage.class);
        Assert.assertEquals(TaskStatus.RESULT_SUCCESS, msgCompletionTaskMessage.getTaskStatus());
        Assert.assertEquals(WORKER_NAME, msgCompletionTaskMessage.getTaskClassifier());
        Assert.assertEquals(WORKER_API_VER, msgCompletionTaskMessage.getTaskApiVersion());
        final TestWorkerResult msgCompletionWorkerResult = codec.deserialise(msgCompletionTaskMessage.getTaskData(), TestWorkerResult.class);
        Assert.assertEquals(SUCCESS, msgCompletionWorkerResult.getResultString());
        Assert.assertTrue(msgCompletionTaskMessage.getContext().containsKey(path.toString()));
        ArrayAsserts.assertArrayEquals(SUCCESS.getBytes(StandardCharsets.UTF_8), msgCompletionTaskMessage.getContext().get(path.toString()));
    }

    /**
     * Test WorkerCore when the input message doesn't even decode to a TaskWrapper - should be an InvalidTaskException *
     */
    @Test(expectedExceptions = InvalidTaskException.class)
    public void testInvalidWrapper()
        throws InvalidNameException, WorkerException, QueueException, CodecException
    {
        BlockingQueue<byte[]> q = new LinkedBlockingQueue<>();
        Codec codec = new JsonCodec();
        WorkerThreadPool wtp = WorkerThreadPool.create(5);
        ConfigurationSource config = Mockito.mock(ConfigurationSource.class);
        ServicePath path = new ServicePath(SERVICE_PATH);
        TestWorkerTask task = new TestWorkerTask();
        TestWorkerQueue queue = new TestWorkerQueueProvider(q).getWorkerQueue(config, 50);
        MessagePriorityManager priorityManager = Mockito.mock(MessagePriorityManager.class);
        Mockito.when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(PRIORITY);
        HealthCheckRegistry healthCheckRegistry = Mockito.mock(HealthCheckRegistry.class);
        TransientHealthCheck transientHealthCheck = Mockito.mock(TransientHealthCheck.class);

        WorkerCore core = new WorkerCore(codec, wtp, queue, priorityManager, getWorkerFactory(task, codec), path, healthCheckRegistry, transientHealthCheck);
        core.start();
        byte[] stuff = codec.serialise("nonsense");
        queue.submitTask(taskInformation, stuff);
    }

    /**
     * Send in a TaskMessage put with an inner task-specific message that cannot be decoded, this should be an INVALID_TASK response *
     */
    @Test
    public void testInvalidTask()
        throws QueueException, InvalidNameException, WorkerException, CodecException, InterruptedException
    {
        BlockingQueue<byte[]> q = new LinkedBlockingQueue<>();
        Codec codec = new JsonCodec();
        WorkerThreadPool wtp = WorkerThreadPool.create(5);
        ConfigurationSource config = Mockito.mock(ConfigurationSource.class);
        ServicePath path = new ServicePath(SERVICE_PATH);
        TestWorkerTask task = new TestWorkerTask();
        TestWorkerQueue queue = new TestWorkerQueueProvider(q).getWorkerQueue(config, 50);
        MessagePriorityManager priorityManager = Mockito.mock(MessagePriorityManager.class);
        Mockito.when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(PRIORITY);
        HealthCheckRegistry healthCheckRegistry = Mockito.mock(HealthCheckRegistry.class);
        TransientHealthCheck transientHealthCheck = Mockito.mock(TransientHealthCheck.class);

        WorkerCore core = new WorkerCore(codec, wtp, queue, priorityManager, getInvalidTaskWorkerFactory(), path, healthCheckRegistry, transientHealthCheck);
        core.start();
        TaskMessage tm = getTaskMessage(task, codec, WORKER_NAME);
        tm.setTaskData(codec.serialise("invalid task data"));
        Map<String, byte[]> context = new HashMap<>();
        String testContext = "test";
        byte[] testContextData = testContext.getBytes(StandardCharsets.UTF_8);
        context.put(testContext, testContextData);
        tm.setContext(context);
        byte[] stuff = codec.serialise(tm);
        queue.submitTask(taskInformation, stuff);
        byte[] result = q.poll(5000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(result);
        TaskMessage taskMessage = codec.deserialise(result, TaskMessage.class);
        Assert.assertEquals(TaskStatus.INVALID_TASK, taskMessage.getTaskStatus());
        Assert.assertEquals(WORKER_NAME, taskMessage.getTaskClassifier());
        Assert.assertEquals(WORKER_API_VER, taskMessage.getTaskApiVersion());
        Assert.assertTrue(taskMessage.getContext().containsKey(testContext));
        ArrayAsserts.assertArrayEquals(testContextData, taskMessage.getContext().get(testContext));
        Assert.assertEquals(QUEUE_OUT, queue.getLastQueue());
    }

    /**
     * Send in a TaskMessage put with an inner task-specific message that cannot be decoded, this should be an
     * INVALID_TASK response. Also inlcude tracking details to verify report update is sent to expected target pipe.
     */
    @Test
    public void testInvalidTaskWithTracking()
            throws QueueException, InvalidNameException, WorkerException, CodecException, InterruptedException
    {
        final BlockingQueue<byte[]> q = new LinkedBlockingQueue<>();
        final Codec codec = new JsonCodec();
        final WorkerThreadPool wtp = WorkerThreadPool.create(5);
        final ConfigurationSource config = Mockito.mock(ConfigurationSource.class);
        final ServicePath path = new ServicePath(SERVICE_PATH);
        final TestWorkerTask task = new TestWorkerTask();
        final TestWorkerQueue queue = new TestWorkerQueueProvider(q).getWorkerQueue(config, 50);
        final MessagePriorityManager priorityManager = Mockito.mock(MessagePriorityManager.class);
        Mockito.when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(PRIORITY);
        final HealthCheckRegistry healthCheckRegistry = Mockito.mock(HealthCheckRegistry.class);
        final TransientHealthCheck transientHealthCheck = Mockito.mock(TransientHealthCheck.class);

        final WorkerCore core = new WorkerCore(codec, wtp, queue, priorityManager, getInvalidTaskWorkerFactory(), path, healthCheckRegistry, transientHealthCheck);
        core.start();

        final TrackingInfo tracking = new TrackingInfo("J23.1.2", new Date(), 0, "http://thehost:1234/job-service/v1/jobs/23/status", "trackingQueue", "trackTo");
        final TaskMessage tm = getTaskMessage(task, codec, WORKER_NAME, tracking);
        tm.setTaskData(codec.serialise("invalid task data"));
        final Map<String, byte[]> context = new HashMap<>();
        final String testContext = "test";
        final byte[] testContextData = testContext.getBytes(StandardCharsets.UTF_8);
        context.put(testContext, testContextData);
        tm.setContext(context);
        final byte[] stuff = codec.serialise(tm);
        queue.submitTask(taskInformation, stuff);

        // Two results expected back. One for the report progress update and another for the message completion.
        //
        // Verify result for the report update call.
        final byte[] rutResult = q.poll(5000, TimeUnit.MILLISECONDS);
        // if the result didn't get back to us, then rutResult will be null
        Assert.assertNotNull(rutResult);
        // deserialise and verify rutResult data
        final TaskMessage rutTaskMessage = codec.deserialise(rutResult, TaskMessage.class);
        Assert.assertEquals(TaskStatus.NEW_TASK, rutTaskMessage.getTaskStatus());
        Assert.assertEquals(TrackingReportConstants.TRACKING_REPORT_TASK_NAME, rutTaskMessage.getTaskClassifier());
        Assert.assertEquals(TrackingReportConstants.TRACKING_REPORT_TASK_API_VER, rutTaskMessage.getTaskApiVersion());
        final TrackingReportTask rutWorkerResult = codec.deserialise(rutTaskMessage.getTaskData(), TrackingReportTask.class);
        Assert.assertEquals("J23.1.2", rutWorkerResult.trackingReports.get(0).jobTaskId);
        Assert.assertEquals(TrackingReportStatus.Failed, rutWorkerResult.trackingReports.get(0).status);
        Assert.assertEquals(TaskStatus.INVALID_TASK.name(), rutWorkerResult.trackingReports.get(0).failure.failureId);
        Assert.assertEquals(WORKER_NAME, rutWorkerResult.trackingReports.get(0).failure.failureSource);
        // Verify result for message completion.
        final byte[] msgCompletionResult = q.poll(5000, TimeUnit.MILLISECONDS);
        // if the result didn't get back to us, then result will be null
        Assert.assertNotNull(msgCompletionResult);
        // deserialise and verify msgCompletionResult data
        final TaskMessage msgCompletionTaskMessage = codec.deserialise(msgCompletionResult, TaskMessage.class);
        Assert.assertEquals(TaskStatus.INVALID_TASK, msgCompletionTaskMessage.getTaskStatus());
        Assert.assertEquals(WORKER_NAME, msgCompletionTaskMessage.getTaskClassifier());
        Assert.assertEquals(WORKER_API_VER, msgCompletionTaskMessage.getTaskApiVersion());
        Assert.assertTrue(msgCompletionTaskMessage.getContext().containsKey(testContext));
        ArrayAsserts.assertArrayEquals(testContextData, msgCompletionTaskMessage.getContext().get(testContext));
        Assert.assertEquals(QUEUE_OUT, queue.getLastQueue());
    }

    /**
     * Send three tasks into a WorkerCore with only two threads, abort them all, check the running ones are interrupted and the other one
     * never starts *
     */
    @Test
    public void testAbortTasks()
        throws CodecException, InterruptedException, WorkerException, ConfigurationException, QueueException, InvalidNameException
    {
        BlockingQueue<byte[]> q = new LinkedBlockingQueue<>();
        Codec codec = new JsonCodec();
        WorkerThreadPool wtp = WorkerThreadPool.create(2);
        ConfigurationSource config = Mockito.mock(ConfigurationSource.class);
        ServicePath path = new ServicePath(SERVICE_PATH);
        TestWorkerTask task = new TestWorkerTask();
        CountDownLatch latch = new CountDownLatch(2);
        TestWorkerQueue queue = new TestWorkerQueueProvider(q).getWorkerQueue(config, 20);
        MessagePriorityManager priorityManager = Mockito.mock(MessagePriorityManager.class);
        Mockito.when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(PRIORITY);
        HealthCheckRegistry healthCheckRegistry = Mockito.mock(HealthCheckRegistry.class);
        TransientHealthCheck transientHealthCheck = Mockito.mock(TransientHealthCheck.class);

        WorkerCore core = new WorkerCore(codec, wtp, queue, priorityManager, getSlowWorkerFactory(latch, task, codec), path, healthCheckRegistry, transientHealthCheck);
        core.start();
        byte[] task1 = codec.serialise(getTaskMessage(task, codec, UUID.randomUUID().toString()));
        byte[] task2 = codec.serialise(getTaskMessage(task, codec, UUID.randomUUID().toString()));
        byte[] task3 = codec.serialise(getTaskMessage(task, codec, UUID.randomUUID().toString()));
        queue.submitTask(getMockTaskInformation("task1"), task1);
        queue.submitTask(getMockTaskInformation("task2"), task2);
        queue.submitTask(getMockTaskInformation("task3"), task3);   // there are only 2 threads, so this task should not even start
        Thread.sleep(500);  // give the test a little breathing room
        queue.triggerAbort();
        latch.await(1, TimeUnit.SECONDS);
        Thread.sleep(100);
        Assert.assertEquals(3, core.getStats().getTasksReceived());
        Assert.assertEquals(3, core.getStats().getTasksAborted());
        Assert.assertEquals(0, core.getStats().getTasksForwarded());
        Assert.assertEquals(0, core.getStats().getTasksDiscarded());
        Assert.assertEquals(0, core.getBacklogSize());
    }

    @Test
    public void testInterupptedTask()
        throws CodecException, WorkerException, ConfigurationException, QueueException, InvalidNameException
    {
        BlockingQueue<byte[]> q = new LinkedBlockingQueue<>();
        Codec codec = new JsonCodec();
        WorkerThreadPool wtp = WorkerThreadPool.create(2);
        ConfigurationSource config = Mockito.mock(ConfigurationSource.class);
        ServicePath path = new ServicePath(SERVICE_PATH);
        TestWorkerTask task = new TestWorkerTask();
        TestWorkerQueue queue = new TestWorkerQueueProvider(q).getWorkerQueue(config, 20);
        MessagePriorityManager priorityManager = Mockito.mock(MessagePriorityManager.class);
        Mockito.when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(PRIORITY);
        HealthCheckRegistry healthCheckRegistry = Mockito.mock(HealthCheckRegistry.class);
        TransientHealthCheck transientHealthCheck = Mockito.mock(TransientHealthCheck.class);

        WorkerCore core = new WorkerCore(codec, wtp, queue, priorityManager, getInterruptedExceptionWorkerFactory(task, codec),
                                         path, healthCheckRegistry, transientHealthCheck);
        core.start();

        final TaskMessage tm = getTaskMessage(task, codec, WORKER_NAME);
        tm.setTaskData(codec.serialise("invalid task data"));

        final byte[] stuff = codec.serialise(tm);
        queue.submitTask(taskInformation, stuff);

        byte[] msgCompletionTaskMessage = null;
        try {
            //give some time for the worker to be pick the task and create a response
            Thread.sleep(10000);
            msgCompletionTaskMessage = q.poll(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Logger.error("InterruptedException" + ex);
        }
        // if the result didn't get back to us, then rutResult will be null
        Assert.assertNotNull(msgCompletionTaskMessage);
        // deserialise and verify rutResult data
        final TaskMessage rutTaskMessage = codec.deserialise(msgCompletionTaskMessage, TaskMessage.class);
        //check if the status is NEW_TASK which qualify as a successful response (but not necessarily a successful result)
        Assert.assertEquals(TaskStatus.RESULT_FAILURE, rutTaskMessage.getTaskStatus());
        Assert.assertEquals(WORKER_API_VER, rutTaskMessage.getTaskApiVersion());
        Assert.assertEquals(QUEUE_OUT, queue.getLastQueue());
    }

    /**
     * Send a message with a statusCheckUrl configured to return a "Paused" status and the worker configured with a non-null paused queue.
     * Verify the message was sent to the paused queue.
     */
    @Test
    public void testPausedTaskWithNonNullPausedQueue()
        throws CodecException, InterruptedException, WorkerException, ConfigurationException, QueueException, InvalidNameException,
               MalformedURLException
    {
        final BlockingQueue<byte[]> q = new LinkedBlockingQueue<>();
        final Codec codec = new JsonCodec();
        final WorkerThreadPool wtp = WorkerThreadPool.create(5);
        final ConfigurationSource config = Mockito.mock(ConfigurationSource.class);
        final ServicePath path = new ServicePath(SERVICE_PATH);
        final TestWorkerTask task = new TestWorkerTask();
        final TestWorkerQueue queue = new TestWorkerQueueProvider(q).getWorkerQueue(config, 50);
        final MessagePriorityManager priorityManager = Mockito.mock(MessagePriorityManager.class);
        Mockito.when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(PRIORITY);
        final HealthCheckRegistry healthCheckRegistry = Mockito.mock(HealthCheckRegistry.class);
        final TransientHealthCheck transientHealthCheck = Mockito.mock(TransientHealthCheck.class);

        final WorkerCore core = new WorkerCore(
            codec, wtp, queue, priorityManager, getWorkerFactory(task, codec), path, healthCheckRegistry, transientHealthCheck);
        core.start();
        // at this point, the queue should hand off the task to the app, the app should get a worker from the mocked WorkerFactory,
        // and the Worker itself is a mock wrapped in a WorkerWrapper, which should return success and the appropriate result data
        final TrackingInfo tracking = new TrackingInfo("J23.1.2", new Date(), 0,
            new File("src/test/resources/paused-status-check-url-response.json").toURI().toURL().toString(), "trackingQueue", "trackTo");
        final byte[] stuff = codec.serialise(getTaskMessage(task, codec, WORKER_NAME, tracking));
        queue.submitTask(taskInformation, stuff);

        // Verify result for paused message.
        final byte[] pausedMsgResult = q.poll(5000, TimeUnit.MILLISECONDS);
        // if the result didn't get back to us, then result will be null
        Assert.assertNotNull(pausedMsgResult);
        // deserialise and verify pausedMsgResult data
        final TaskMessage pausedMsgTaskMessage = codec.deserialise(pausedMsgResult, TaskMessage.class);
        Assert.assertEquals(pausedMsgTaskMessage.getTaskStatus(), TaskStatus.NEW_TASK);
        Assert.assertEquals(pausedMsgTaskMessage.getTaskClassifier(), WORKER_NAME);
        Assert.assertEquals(pausedMsgTaskMessage.getTaskApiVersion(), WORKER_API_VER);
        Assert.assertEquals(queue.getLastQueue(), QUEUE_PAUSED);
        final TestWorkerTask testWorkerTask = codec.deserialise(pausedMsgTaskMessage.getTaskData(), TestWorkerTask.class);
        Assert.assertEquals(testWorkerTask.getData(), "test123");
    }

    /**
     * Send a message with a statusCheckUrl configured to return a "Paused" status but the worker configured with a null paused queue.
     * Verify the message was NOT sent to the paused queue, but processed as normal.
     */
    @Test
    public void testPausedTaskWithNullPausedQueue()
        throws CodecException, InterruptedException, WorkerException, ConfigurationException, QueueException, InvalidNameException,
               MalformedURLException
    {
        final BlockingQueue<byte[]> q = new LinkedBlockingQueue<>();
        final Codec codec = new JsonCodec();
        final WorkerThreadPool wtp = WorkerThreadPool.create(5);
        final ConfigurationSource config = Mockito.mock(ConfigurationSource.class);
        final ServicePath path = new ServicePath(SERVICE_PATH);
        final TestWorkerTask task = new TestWorkerTask();
        final TestWorkerQueue queue = new TestWorkerQueueWithNullPausedQueueProvider(q).getWorkerQueue(config, 50);
        final MessagePriorityManager priorityManager = Mockito.mock(MessagePriorityManager.class);
        Mockito.when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(PRIORITY);
        final HealthCheckRegistry healthCheckRegistry = Mockito.mock(HealthCheckRegistry.class);
        final TransientHealthCheck transientHealthCheck = Mockito.mock(TransientHealthCheck.class);

        final WorkerCore core = new WorkerCore(
            codec, wtp, queue, priorityManager, getWorkerFactory(task, codec), path, healthCheckRegistry, transientHealthCheck);
        core.start();
        // at this point, the queue should hand off the task to the app, the app should get a worker from the mocked WorkerFactory,
        // and the Worker itself is a mock wrapped in a WorkerWrapper, which should return success and the appropriate result data
        final TrackingInfo tracking = new TrackingInfo("J23.1.2", new Date(), 0,
            new File("src/test/resources/paused-status-check-url-response.json").toURI().toURL().toString(), "trackingQueue", "trackTo");
        final byte[] stuff = codec.serialise(getTaskMessage(task, codec, WORKER_NAME, tracking));
        queue.submitTask(taskInformation, stuff);

        // Two results expected back. One for the report progress update and another for the message completion.
        //
        // Verify result for the report update call.
        final byte[] rutResult = q.poll(5000, TimeUnit.MILLISECONDS);
        Assert.assertNotEquals(queue.getLastQueue(), QUEUE_PAUSED);
        // if the result didn't get back to us, then rutResult will be null
        Assert.assertNotNull(rutResult);
        // deserialise and verify rutResult data
        final TaskMessage rutTaskMessage = codec.deserialise(rutResult, TaskMessage.class);
        Assert.assertEquals(TaskStatus.NEW_TASK, rutTaskMessage.getTaskStatus());
        Assert.assertEquals(TrackingReportConstants.TRACKING_REPORT_TASK_NAME, rutTaskMessage.getTaskClassifier());
        Assert.assertEquals(TrackingReportConstants.TRACKING_REPORT_TASK_API_VER, rutTaskMessage.getTaskApiVersion());
        final TrackingReportTask rutWorkerResult = codec.deserialise(rutTaskMessage.getTaskData(), TrackingReportTask.class);
        Assert.assertEquals("J23.1.2", rutWorkerResult.trackingReports.get(0).jobTaskId);
        Assert.assertEquals(TrackingReportStatus.Progress, rutWorkerResult.trackingReports.get(0).status);
        // Verify result for message completion.
        final byte[] msgCompletionResult = q.poll(5000, TimeUnit.MILLISECONDS);
        Assert.assertNotEquals(queue.getLastQueue(), QUEUE_PAUSED);
        // if the result didn't get back to us, then result will be null
        Assert.assertNotNull(msgCompletionResult);
        // deserialise and verify msgCompletionResult data
        final TaskMessage msgCompletionTaskMessage = codec.deserialise(msgCompletionResult, TaskMessage.class);
        Assert.assertEquals(TaskStatus.RESULT_SUCCESS, msgCompletionTaskMessage.getTaskStatus());
        Assert.assertEquals(WORKER_NAME, msgCompletionTaskMessage.getTaskClassifier());
        Assert.assertEquals(WORKER_API_VER, msgCompletionTaskMessage.getTaskApiVersion());
        final TestWorkerResult msgCompletionWorkerResult = codec.deserialise(msgCompletionTaskMessage.getTaskData(), TestWorkerResult.class);
        Assert.assertEquals(SUCCESS, msgCompletionWorkerResult.getResultString());
        Assert.assertTrue(msgCompletionTaskMessage.getContext().containsKey(path.toString()));
        ArrayAsserts.assertArrayEquals(SUCCESS.getBytes(StandardCharsets.UTF_8), msgCompletionTaskMessage.getContext().get(path.toString()));
    }

    private TaskMessage getTaskMessage(final TestWorkerTask task, final Codec codec, final String taskId)
        throws CodecException
    {
        return getTaskMessage(task, codec, taskId, null);
    }

    private TaskMessage getTaskMessage(final TestWorkerTask task, final Codec codec, final String taskId, final TrackingInfo tracking)
        throws CodecException
    {
        TaskMessage tm = new TaskMessage();
        tm.setTaskId(taskId);
        tm.setTaskStatus(TaskStatus.NEW_TASK);
        tm.setTaskClassifier(WORKER_NAME);
        tm.setTaskApiVersion(WORKER_API_VER);
        tm.setTaskData(codec.serialise(task));
        tm.setTo(QUEUE_IN);
        tm.setTracking(tracking);
        return tm;
    }
    
    private QueueTaskMessage getQueueTaskMessage(final TestWorkerTask task, final String taskId)
    {
        QueueTaskMessage tm = new QueueTaskMessage();
        tm.setTaskId(taskId);
        tm.setTaskStatus(TaskStatus.NEW_TASK);
        tm.setTaskClassifier(WORKER_NAME);
        tm.setTaskApiVersion(WORKER_API_VER);
        tm.setTaskData(task);
        tm.setTo(QUEUE_IN);
        tm.setTracking(null);
        return tm;
    }

    private WorkerFactory getWorkerFactory(final TestWorkerTask task, final Codec codec)
        throws WorkerException
    {
        WorkerFactory factory = Mockito.mock(WorkerFactory.class);
        Worker mockWorker = getWorker(task, codec);
        Mockito.when(factory.getWorker(Mockito.any())).thenReturn(mockWorker);
        return factory;
    }

    private WorkerFactory getInvalidTaskWorkerFactory()
        throws WorkerException
    {
        WorkerFactory factory = Mockito.mock(WorkerFactory.class);
        Mockito.when(factory.getWorker(Mockito.any())).thenThrow(InvalidTaskException.class);
        Mockito.when(factory.getInvalidTaskQueue()).thenReturn(QUEUE_OUT);
        return factory;
    }
    
    private WorkerFactory getInterruptedExceptionWorkerFactory(final TestWorkerTask task, final Codec codec)
        throws WorkerException
    {
        WorkerFactory factory = Mockito.mock(WorkerFactory.class);
        Worker mockWorker = new InterruptedExceptionWorker(task, codec);
        Mockito.when(factory.getWorker(Mockito.any())).thenReturn(mockWorker);
        return factory;
    }

    private WorkerFactory getSlowWorkerFactory(final CountDownLatch latch, final TestWorkerTask task, final Codec codec)
        throws WorkerException
    {
        WorkerFactory factory = Mockito.mock(WorkerFactory.class);
        Worker mockWorker = new SlowWorker(task, QUEUE_OUT, codec, latch);
        Mockito.when(factory.getWorker(Mockito.any())).thenReturn(mockWorker);
        return factory;
    }

    private Worker getWorker(final TestWorkerTask task, final Codec codec)
        throws InvalidTaskException
    {
        return new AbstractWorker<TestWorkerTask, TestWorkerResult>(task, QUEUE_OUT, codec, Mockito.mock(WorkerTaskData.class))
        {
            @Override
            public WorkerResponse doWork()
            {
                TestWorkerResult result = new TestWorkerResult();
                result.setResultString(SUCCESS);
                return createSuccessResult(result, SUCCESS.getBytes(StandardCharsets.UTF_8));
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

    private class TestWorkerQueueProvider implements WorkerQueueProvider
    {
        private final BlockingQueue<byte[]> results;

        public TestWorkerQueueProvider(final BlockingQueue<byte[]> results)
        {
            this.results = results;
        }

        @Override
        public final TestWorkerQueue getWorkerQueue(final ConfigurationSource configurationSource, final int maxTasks)
        {
            return new TestWorkerQueue(this.results);
        }
    }

    private class TestWorkerQueue implements ManagedWorkerQueue
    {
        private TaskCallback callback;
        private final BlockingQueue<byte[]> results;
        private String lastQueue;

        public TestWorkerQueue(final BlockingQueue<byte[]> results)
        {
            this.results = results;
        }

        @Override
        public void start(final TaskCallback callback)
            throws QueueException
        {
            this.callback = Objects.requireNonNull(callback);
        }
        
        @Override
        public void publish(TaskInformation taskInformation, byte[] taskMessage, String targetQueue, Map<String, Object> headers, int priority, boolean isLastMessage)
            throws QueueException
        {
            this.lastQueue = targetQueue;
            results.offer(taskMessage);
        }

        @Override
        public void publish(TaskInformation taskInformation, byte[] taskMessage, String targetQueue, Map<String, Object> headers, int priority)
        {
            this.lastQueue = targetQueue;
            results.offer(taskMessage);
        }

        @Override
        public void publish(TaskInformation taskInformation, byte[] taskMessage, String targetQueue, Map<String, Object> headers)
            throws QueueException
        {
            this.lastQueue = targetQueue;
            results.offer(taskMessage);
        }

        @Override
        public void rejectTask(final TaskInformation taskInformation)
        {
            System.out.println("Rejecting the test work");
            this.lastQueue = QUEUE_OUT;
            Codec codec = new JsonCodec();
            TaskMessage tm = new TaskMessage();
            tm.setTaskStatus(TaskStatus.RESULT_FAILURE);
            tm.setTaskClassifier(WORKER_NAME);
            tm.setTaskApiVersion(WORKER_API_VER);
            tm.setTo(QUEUE_OUT);
            tm.setTracking(null);
            try {
                tm.setTaskData(codec.serialise(taskInformation.getInboundMessageId().getBytes()));
                results.offer(codec.serialise(tm));
            } catch (CodecException ex) {
                Logger.error("CodecException" + ex);
            }
        }

        @Override
        public void discardTask(TaskInformation taskInformation)
        {
        }

        @Override
        public void acknowledgeTask(TaskInformation taskInformation)
        {
        }

        @Override
        public String getInputQueue()
        {
            return QUEUE_IN;
        }

        @Override
        public String getPausedQueue()
        {
            return QUEUE_PAUSED;
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

        public String getLastQueue()
        {
            return lastQueue;
        }

        public void triggerAbort()
        {
            callback.abortTasks();
        }

        public void submitTask(final TaskInformation taskInformation, final byte[] stuff)
            throws WorkerException
        {
            callback.registerNewTask(taskInformation, stuff, new HashMap<>());
        }

        @Override
        public void disconnectIncoming()
        {
        }

        @Override
        public void reconnectIncoming()
        {
        }
    }

    private class TestWorkerQueueWithNullPausedQueueProvider implements WorkerQueueProvider
    {
        private final BlockingQueue<byte[]> results;

        public TestWorkerQueueWithNullPausedQueueProvider(final BlockingQueue<byte[]> results)
        {
            this.results = results;
        }

        @Override
        public final TestWorkerQueueWithNullPausedQueue getWorkerQueue(final ConfigurationSource configurationSource, final int maxTasks)
        {
            return new TestWorkerQueueWithNullPausedQueue(this.results);
        }
    }

    private class TestWorkerQueueWithNullPausedQueue extends TestWorkerQueue
    {
        public TestWorkerQueueWithNullPausedQueue(final BlockingQueue<byte[]> results)
        {
            super(results);
        }

        @Override
        public String getPausedQueue()
        {
            return null;
        }
    }

    private class SlowWorker extends AbstractWorker<TestWorkerTask, TestWorkerResult>
    {
        private final CountDownLatch latch;

        public SlowWorker(final TestWorkerTask task, final String resultQueue, final Codec codec, final CountDownLatch latch)
            throws WorkerException
        {
            super(task, resultQueue, codec, Mockito.mock(WorkerTaskData.class));
            this.latch = Objects.requireNonNull(latch);
        }

        @Override
        public WorkerResponse doWork()
            throws InterruptedException
        {
            try {
                System.out.println("Starting test work");
                Thread.sleep(10000);
                TestWorkerResult result = new TestWorkerResult();
                result.setResultString(SUCCESS);
                return createSuccessResult(result);
            } catch (InterruptedException e) {
                System.out.println("Test work interrupted");
                latch.countDown();
                throw e;
            }
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
    }

    private class InterruptedExceptionWorker extends AbstractWorker<TestWorkerTask, TestWorkerResult>
    {

        public InterruptedExceptionWorker(final TestWorkerTask task, final Codec codec)
            throws WorkerException
        {
            super(task, QUEUE_OUT, codec, Mockito.mock(WorkerTaskData.class));
        }

        @Override
        public WorkerResponse doWork()
            throws InterruptedException
        {
            System.out.println("Starting InterruptedExceptionWorker test work");
            Thread.sleep(1000);
            Thread.currentThread().interrupt();
            checkIfInterrupted();
            TestWorkerResult result = new TestWorkerResult();
            result.setResultString(FAILURE);
            return createFailureResult(result);
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
    }
    
    TaskInformation getMockTaskInformation(final String inboundMessageId){
        final TaskInformation taskInformation = mock(TaskInformation.class);
        when(taskInformation.getInboundMessageId()).thenReturn(inboundMessageId);

        return taskInformation;
    }
}
