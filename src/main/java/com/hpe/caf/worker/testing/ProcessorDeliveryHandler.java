package com.hpe.caf.worker.testing;

import com.google.common.base.Strings;
import com.hpe.caf.api.worker.TaskMessage;
import org.apache.commons.lang3.exception.ExceptionUtils;

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

        TestItem testItem;

            String inputIdentifier = resultProcessor.getInputIdentifier(taskMessage);
            if (Strings.isNullOrEmpty(inputIdentifier)) {
                testItem = itemStore.find(taskMessage.getTaskId());
            } else {
                testItem = itemStore.find(inputIdentifier);
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
                context.failed(testItem, "Item " + testItem.getTag() + ": Result processor didn't return success. Result processor name: " + resultProcessor.getClass().getName() + "\nNo detailed message returned.");
                testItem.setCompleted(true);
            }
        }
        catch (Throwable e) {
            e.printStackTrace();
            context.failed(testItem, buildFailedMessage(testItem, e));
            testItem.setCompleted(true);
        }

        if (testItem.isCompleted()) {
            itemStore.remove(taskMessage.getTaskId());
        }
        checkForFinished();
    }

    private String buildFailedMessage(TestItem testItem, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append("Test case failed.");
        TestCaseInfo info = testItem.getTestCaseInformation();
        if (info != null) {
            sb.append(" Test case id: " + info.getTestCaseId());
            sb.append("\nTest case description: " + info.getDescription());
            sb.append("\nTest case comments: " + info.getComments());
            sb.append("\nTest case associated tickets: " + info.getAssociatedTickets());
            sb.append("\n");
        }
        sb.append("Message: " + ExceptionUtils.getMessage(throwable));
        sb.append("\n");
        sb.append("Root cause message: " + ExceptionUtils.getRootCauseMessage(throwable));

        sb.append("\nStack trace:\n");
        sb.append(ExceptionUtils.getStackTrace(throwable));
        return sb.toString();
    }

    private void checkForFinished() {
        if (itemStore.size() == 0) {
            context.finishedSuccessfully();
        }
    }

}
