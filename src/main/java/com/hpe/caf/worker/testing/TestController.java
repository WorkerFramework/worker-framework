package com.hpe.caf.worker.testing;

import com.hpe.caf.api.worker.TaskMessage;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

/**
 * Created by ploch on 08/11/2015.
 */
public class TestController implements Closeable {

    private final WorkerServices workerServices;
    private final TestItemProvider itemProvider;
    private final QueueManager queueManager;
    private final WorkerTaskFactory taskFactory;
    private final ResultProcessor resultProcessor;
    Thread thread;

    public TestController(WorkerServices workerServices, TestItemProvider itemProvider, QueueManager queueManager, WorkerTaskFactory taskFactory, ResultProcessor resultProcessor) {
        this.workerServices = workerServices;

        this.itemProvider = itemProvider;
        this.queueManager = queueManager;
        this.taskFactory = taskFactory;
        this.resultProcessor = resultProcessor;
    }

    public void runTests() throws Exception {

        System.out.println("===============  Starting tests ======================");

        Collection<TestItem> items = itemProvider.getItems();

        if (items.size() == 0){
            throw new Exception("No test items provided! Exiting.");
        }

        ExecutionContext context = new ExecutionContext();

        thread = queueManager.start(new ProcessorDeliveryHandler(resultProcessor, context));

        TaskMessageFactory messageFactory = new TaskMessageFactory(workerServices.getCodec(), taskFactory.getWorkerName(), taskFactory.getApiVersion());

        for (TestItem item : items) {
            Object workerTask = taskFactory.createTask(item);
            //String taskId = UUID.randomUUID().toString();
            String taskId = item.getTag() == null ? UUID.randomUUID().toString() : item.getTag();
            TaskMessage message = messageFactory.create(workerTask, taskId);

            context.getItemStore().store(taskId, item);
            System.out.println("================================================================================");
            System.out.println(" QUEUEING NEW TASK: " + item.getTag());
            System.out.println("================================================================================");
            queueManager.publish(message);
        }

        TestResult result = context.getTestResult();

        if (!result.isSuccess()) {
            throw new Exception(result.getErrorMessage());
        }

        System.out.println("===============  Finished successfully ======================");
    }

    @Override
    public void close() throws IOException {
        try {
            queueManager.close();
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
