package com.hpe.caf.worker.testing;

import com.google.common.base.Strings;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;

import java.io.IOException;

/**
 * Created by ploch on 08/11/2015.
 */
public class ProcessorDeliveryHandler implements ResultHandler {

    private final TestItemStore itemStore;
    private final ResultProcessor resultProcessor;
    private ExecutionContext context;
    private QueueManager queueManager;

    public ProcessorDeliveryHandler(ResultProcessor resultProcessor, ExecutionContext context, QueueManager queueManager) {

        this.itemStore = context.getItemStore();
        this.resultProcessor = resultProcessor;
        this.context = context;
        this.queueManager = queueManager;
    }

    @Override
    public void handleResult(TaskMessage taskMessage) {

        if(this.queueManager.isDebugEnabled()) {
            try {
                queueManager.publishDebugOutput(taskMessage);
            } catch (CodecException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("New delivery: task id: " + taskMessage.getTaskId() + ", status: " + taskMessage.getTaskStatus());

        TestItem testItem = null;
        try {
            String inputIdentifier = resultProcessor.getInputIdentifier(taskMessage);
            if (Strings.isNullOrEmpty(inputIdentifier)) {
                testItem = itemStore.find(taskMessage.getTaskId());
            } else {
                testItem = itemStore.find(inputIdentifier);
            }
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
                testItem.setCompleted(true);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            context.failed(e.getMessage());
        }

        if (testItem.isCompleted()) {
            itemStore.remove(taskMessage.getTaskId());
            testItem.setCompleted(true);
        }
        checkForFinished();
    }

    private void checkForFinished() {
        if (itemStore.size() == 0) {
            context.finishedSuccessfully();
        }
    }

}
