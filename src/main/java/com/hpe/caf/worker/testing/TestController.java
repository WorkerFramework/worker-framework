package com.hpe.caf.worker.testing;

import com.hpe.caf.api.worker.TaskMessage;

import java.util.Collection;
import java.util.UUID;

/**
 * Created by ploch on 08/11/2015.
 */
public class TestController {

    private final WorkerServices workerServices;
    private final TestItemProvider itemProvider;
    private final QueueManager queueManager;
    private final WorkerTaskFactory taskFactory;
    private final ResultProcessor resultProcessor;

    public TestController(WorkerServices workerServices, TestItemProvider itemProvider, QueueManager queueManager,/* TestItemStore itemStore,*/ WorkerTaskFactory taskFactory, ResultProcessor resultProcessor) {
        this.workerServices = workerServices;

        this.itemProvider = itemProvider;
        this.queueManager = queueManager;
        this.taskFactory = taskFactory;
        this.resultProcessor = resultProcessor;
    }

    public void runTests() throws Exception {
        Collection<TestItem> items = itemProvider.getItems();

        ExecutionContext context = new ExecutionContext();
        queueManager.start(new ProcessorDeliveryHandler(resultProcessor, context));

        TaskMessageFactory messageFactory = new TaskMessageFactory(workerServices.getCodec(), taskFactory.getWorkerName(), taskFactory.getApiVersion());

        for (TestItem item : items) {
            Object workerTask = taskFactory.createTask(item);
            String taskId = UUID.randomUUID().toString();
            TaskMessage message = messageFactory.create(workerTask, taskId);
            context.getItemStore().store(taskId, item);
            queueManager.publish(message);
        }

        TestResult result = context.getTestResult();

        if (!result.isSuccess()) {
            throw new Exception(result.getErrorMessage());
        }

        System.out.println("Finished successfully");
    }

}
