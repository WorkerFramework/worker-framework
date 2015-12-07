package com.hpe.caf.worker.testing;

import com.hpe.caf.api.worker.TaskMessage;

/**
 * Created by ploch on 08/11/2015.
 */
public class ProcessorDeliveryHandler implements ResultHandler {

    private final TestItemStore itemStore;
    private final ResultProcessor resultProcessor;
    private ExecutionContext context;

    public ProcessorDeliveryHandler(ResultProcessor resultProcessor, ExecutionContext context) {

        this.itemStore = context.getItemStore();
        this.resultProcessor = resultProcessor;
        this.context = context;
    }

    @Override
    public void handleResult(TaskMessage taskMessage) {

        System.out.println("New delivery: task id: " + taskMessage.getTaskId() + ", status: " + taskMessage.getTaskStatus());
       // TestItem testItem = itemStore.findAndRemove(taskMessage.getTaskId());
        TestItem testItem = null;
        try {
            testItem = itemStore.find(taskMessage.getTaskId());
        } catch (Throwable e) {
            e.printStackTrace();
            context.failed(e.getMessage());
        }
        if (testItem == null) {
            System.out.println("Item with id " + taskMessage.getTaskId() + " was not found. Skipping.");
            checkForFinished();
            return;
        }

        try {
            boolean success = resultProcessor.process(testItem, taskMessage);
            System.out.println("Item " + testItem.getTag() + ": Result processor success: " + success);
            if (!success) {
                context.failed("Item " + testItem.getTag() + ": Result processor didn't return success. Result processor name: " + resultProcessor.getClass().getName());
                return;
            }
            if (testItem.isCompleted()) {
                itemStore.remove(taskMessage.getTaskId());
            }
            checkForFinished();
        } catch (Throwable e) {
            e.printStackTrace();
            context.failed(e.getMessage());
        }
    }

    private void checkForFinished() {
        if (itemStore.size() == 0) {
            context.finishedSuccessfully();
        }
    }

}
