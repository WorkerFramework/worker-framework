package com.hpe.caf.worker.testing;

import com.google.common.base.Strings;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TrackingInfo;

import java.io.Closeable;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by oloughli on 31/05/2016.
 */
public class TestControllerSingle implements Closeable {

    private final WorkerServices workerServices;
    private final QueueManager queueManager;
    private final WorkerTaskFactory taskFactory;
    private final ResultProcessor resultProcessor;
    private final boolean stopOnError;
    private final TestResultsReporter resultsReporter;
    private final long defaultTimeOutMs = 240000; //4 minutes
    /**
     * The Thread.
     */
    Thread thread;

    public TestControllerSingle(WorkerServices workerServices, QueueManager queueManager, WorkerTaskFactory taskFactory, ResultProcessor resultProcessor, boolean stopOnError) {
        this(workerServices, queueManager, taskFactory, resultProcessor, stopOnError, new ConsoleTestReporter());
    }

    /**
     * Instantiates a new Test controller.
     *
     * @param workerServices  the worker services
     * @param queueManager    the worker queue manager
     * @param taskFactory     the worker task factory
     * @param resultProcessor the worker result processor
     * @param stopOnError     determines if tests should continue after any validation error
     */
    public TestControllerSingle(WorkerServices workerServices, QueueManager queueManager, WorkerTaskFactory taskFactory, ResultProcessor resultProcessor, boolean stopOnError, TestResultsReporter resultsReporter) {
        this.workerServices = workerServices;
        this.queueManager = queueManager;
        this.taskFactory = taskFactory;
        this.resultProcessor = resultProcessor;
        this.stopOnError = stopOnError;
        this.resultsReporter = resultsReporter;
    }

    public void initialise() throws Exception{
        queueManager.initialise();
    }

    public void runTests(TestItem testItem) throws Exception
    {
        System.out.println("\n===============  Starting test Item "+ testItem.getTag() +" ======================");
        ExecutionContext context = new ExecutionContext(stopOnError);

        thread = queueManager.startConsumer((new ProcessorDeliveryHandler(resultProcessor, context, queueManager)));
        String timeoutSetting = SettingsProvider.defaultProvider.getSetting(SettingNames.timeOutMs);
        long timeout = timeoutSetting == null ? defaultTimeOutMs : Long.parseLong(timeoutSetting);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
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
    public void close() throws IOException {
        try {
            queueManager.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void closeConsumer() throws IOException{
        queueManager.closeConsumer();
    }
}