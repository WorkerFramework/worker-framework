package com.hpe.caf.worker.testing;

import com.google.common.base.Strings;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TrackingInfo;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@code TestController} class responsible for executing test cases and controlling execution.
 * {@code TestController} will:
 * <ol>
 *     <li>Load {@link TestItem} instances using {@link TestItemProvider} implementation. These are either full test cases
 *     or stubs for generating test data
 *     <li>Start the {@link QueueManager} which manages interactions with RabbitMQ (publishing and listening to result messages)
 *     <li>Create a worker task for each {@code TestItem} using {@link WorkerTaskFactory} implementation
 *     <li>Publish worker tasks to the RabbitMQ
 * </ol>
 *
 * Queued messages will be processed by a worker and published to results queue (by a worker under test). Result messages
 * are processed by implementation of {@link ResultProcessor}.
 */
public class TestController implements Closeable {

    private final WorkerServices workerServices;
    private final TestItemProvider itemProvider;
    private final QueueManager queueManager;
    private final WorkerTaskFactory taskFactory;
    private final ResultProcessor resultProcessor;
    private final boolean stopOnError;
    private final TestResultsReporter resultsReporter;
    private final long defaultTimeOutMs = 600000; // 10 minutes
    /**
     * The Thread.
     */
    Thread thread;

    /**
     * Instantiates a new Test controller.
     *  @param workerServices  the worker services
     * @param itemProvider    the {@link TestItem} provider (test cases)
     * @param queueManager    the worker queue manager
     * @param taskFactory     the worker task factory
     * @param resultProcessor the worker result processor
     * @param stopOnError     determines if tests should continue after any validation error
     */
    public TestController(WorkerServices workerServices, TestItemProvider itemProvider, QueueManager queueManager, WorkerTaskFactory taskFactory, ResultProcessor resultProcessor, boolean stopOnError) {
        this(workerServices, itemProvider, queueManager, taskFactory, resultProcessor, stopOnError, new ConsoleTestReporter());
    }

    /**
     * Instantiates a new Test controller.
     *
     * @param workerServices  the worker services
     * @param itemProvider    the {@link TestItem} provider (test cases)
     * @param queueManager    the worker queue manager
     * @param taskFactory     the worker task factory
     * @param resultProcessor the worker result processor
     * @param stopOnError     determines if tests should continue after any validation error
     */
    public TestController(WorkerServices workerServices, TestItemProvider itemProvider, QueueManager queueManager, WorkerTaskFactory taskFactory, ResultProcessor resultProcessor, boolean stopOnError, TestResultsReporter resultsReporter) {
        this.workerServices = workerServices;

        this.itemProvider = itemProvider;
        this.queueManager = queueManager;
        this.taskFactory = taskFactory;
        this.resultProcessor = resultProcessor;
        this.stopOnError = stopOnError;
        this.resultsReporter = resultsReporter;
    }

    /**
     * Executes the tests using types provided.
     * <ul>
     * <li>Retrieves test cases - {@link TestItem} instances from {@link TestItemProvider}
     * <li>Starts {@link QueueManager}
     * <li>Iterates over test cases and uses them to create and publish worker tasks
     * <li>Stores test case data in {@link TestItemStore} so they can be retrieved when worker finishes
     * task processing
     * </ul>
     *
     * @throws Exception the exception
     */
    public void runTests() throws Exception {

        System.out.println("===============  Starting tests ======================");

        Collection<TestItem> items = itemProvider.getItems();

        if (items.size() == 0) {
            throw new Exception("No test items provided! Exiting.");
        }

        ExecutionContext context = new ExecutionContext(stopOnError);

        String timeoutSetting = SettingsProvider.defaultProvider.getSetting(SettingNames.timeOutMs);
        long timeout = timeoutSetting == null ? defaultTimeOutMs : Long.parseLong(timeoutSetting);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                context.testRunsTimedOut();
            }
        }, timeout);
        thread = queueManager.start(new ProcessorDeliveryHandler(resultProcessor, context, queueManager));

        TaskMessageFactory messageFactory = new TaskMessageFactory(workerServices.getCodec(), taskFactory.getWorkerName(), queueManager.getWorkerInputQueueName(), taskFactory.getApiVersion());

        HashMap<String, TestItem> storedItems = new HashMap<>();

        for (TestItem item : items) {
            String taskId = item.getTag() == null ? UUID.randomUUID().toString() : item.getTag();

            String inputIdentifier = item.getInputIdentifier();
            if (Strings.isNullOrEmpty(inputIdentifier)) {
                context.getItemStore().store(taskId, item);
            } else {
                context.getItemStore().store(item.getInputIdentifier(), item);
            }
            storedItems.put(taskId, item);
        }

        for (Map.Entry<String, TestItem> entry : storedItems.entrySet()) {
            TestItem item = entry.getValue();
            String taskId = entry.getKey();
            Object workerTask = taskFactory.createTask(item);
            TrackingInfo tracking = taskFactory.createTrackingInfo(item);
            TaskMessage message = messageFactory.create(workerTask, tracking, taskId);
            System.out.println("================================================================================");
            System.out.println(" QUEUEING NEW TASK: " + item.getTag());
            System.out.println("================================================================================");
            queueManager.publish(message);
        }

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
}
