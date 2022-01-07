/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.testing;

import java.io.Closeable;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.google.common.base.Strings;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TrackingInfo;

/**
 * Created by oloughli on 31/05/2016.
 */
public class TestControllerSingle implements Closeable
{
    private final WorkerServices workerServices;
    private final QueueManager queueManager;
    private final WorkerTaskFactory taskFactory;
    private final ResultProcessor resultProcessor;
    private final boolean stopOnError;
    private final TestResultsReporter resultsReporter;
    private final long defaultTimeOutMs = 600000; //10 minutes
    private final ExecutionContext context;
    /**
     * The Thread.
     */
    Thread thread;

    public TestControllerSingle(WorkerServices workerServices, QueueManager queueManager, WorkerTaskFactory taskFactory, ResultProcessor resultProcessor, boolean stopOnError)
    {
        this(workerServices, queueManager, taskFactory, resultProcessor, stopOnError, new ConsoleTestReporter());
    }

    /**
     * Instantiates a new Test controller.
     *
     * @param workerServices the worker services
     * @param queueManager the worker queue manager
     * @param taskFactory the worker task factory
     * @param resultProcessor the worker result processor
     * @param stopOnError determines if tests should continue after any validation error
     */
    public TestControllerSingle(WorkerServices workerServices, QueueManager queueManager, WorkerTaskFactory taskFactory, ResultProcessor resultProcessor, boolean stopOnError, TestResultsReporter resultsReporter)
    {
        this.workerServices = workerServices;
        this.queueManager = queueManager;
        this.taskFactory = taskFactory;
        this.resultProcessor = resultProcessor;
        this.stopOnError = stopOnError;
        this.resultsReporter = resultsReporter;
        context = new ExecutionContext(stopOnError);
    }

    public void initialise() throws Exception
    {
        try {
            queueManager.start((new ProcessorDeliveryHandler(resultProcessor, context, queueManager)));
        } catch (Throwable e) {
            System.out.println("Exception happened during queue initialization: " + e.toString());
            e.printStackTrace();
            throw e;
        }
    }

    public void runTests(TestItem testItem) throws Exception
    {
        System.out.println("\n===============  Starting test Item " + testItem.getTag() + " ======================");

        context.initializeContext();
        queueManager.purgeQueues();

        String timeoutSetting = SettingsProvider.defaultProvider.getSetting(SettingNames.timeOutMs);
        long timeout = timeoutSetting == null ? defaultTimeOutMs : Long.parseLong(timeoutSetting);

        Timer timer = new Timer();
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                context.testRunsTimedOut();
            }
        }, timeout);

        TaskMessageFactory messageFactory = new TaskMessageFactory(workerServices.getCodec(), taskFactory.getWorkerName(), queueManager.getWorkerInputQueueName(), taskFactory.getApiVersion());

        String taskId = testItem.getTag() == null ? UUID.randomUUID().toString() : testItem.getTag();

        String inputIdentifier = testItem.getInputIdentifier();
        if (Strings.isNullOrEmpty(inputIdentifier)) {
            context.getItemStore().store(taskId, testItem);
        } else {
            context.getItemStore().store(testItem.getInputIdentifier(), testItem);
        }

        Object workerTask = taskFactory.createTask(testItem);
        TrackingInfo tracking = taskFactory.createTrackingInfo(testItem);
        TaskMessage message = messageFactory.create(workerTask, tracking, taskId);
        System.out.println("================================================================================");
        System.out.println(" QUEUEING NEW TASK: " + testItem.getTag());
        System.out.println("================================================================================");
        queueManager.publish(message);

        TestResult result = context.getTestResult();
        timer.cancel();
        resultsReporter.reportResults(result);
        if (!result.isSuccess()) {
            throw new TestsFailedException(result.getErrorMessage(), result.getResults());
        }

        System.out.println("===============  Finished successfully ======================");
    }

    @Override
    public void close() throws IOException
    {
        try {
            queueManager.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
