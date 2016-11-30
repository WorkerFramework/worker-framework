package com.hpe.caf.worker.testing;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.TaskMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ploch on 08/11/2015.
 */
public abstract class OutputToFileProcessor<TResult, TInput, TExpected> extends AbstractResultProcessor<TResult, TInput, TExpected> {

    private final String outputFolder;

    /**
     * Getter for property 'outputFolder'.
     *
     * @return Value for property 'outputFolder'.
     */
    protected String getOutputFolder() {
        return outputFolder;
    }

    protected OutputToFileProcessor(final Codec codec, final Class<TResult> resultClass, final String outputFolder) {
        super(codec, resultClass);
        this.outputFolder = outputFolder;
    }

    @Override
    protected boolean processWorkerResult(TestItem<TInput, TExpected> testItem, TaskMessage message, TResult result) throws Exception {
        byte[] content = getOutputContent(result, message, testItem);
        return processResult(testItem, message,content);
    }

    @Override
    protected boolean processFailedWorkerResult(TestItem<TInput, TExpected> testItem, TaskMessage message, Map<String, Object> result) throws Exception {
        byte[] content = getFailedOutputContent(message, testItem);
        return processResult(testItem, message,content);
    }

    public boolean processResult(TestItem<TInput, TExpected> testItem, TaskMessage message, byte[] content) throws IOException {
        Path filePath = getSaveFilePath(testItem, message);
        Files.deleteIfExists(filePath);
        while(Files.exists(filePath)); // Wait till the file is really deleted.
        Files.write(filePath, content, StandardOpenOption.CREATE);
        return true;
    }

    // move to base type and rename to getSerializedTestItem(testItem)
    public byte[] getSerializedTestItem(TestItem<TInput, TExpected> testItem, TestConfiguration configuration) throws Exception {
        TestCaseInfo info = new TestCaseInfo();
        Matcher matcher = Pattern.compile(".*[/\\\\]").matcher(testItem.getTag());
        if (matcher.find()) {
            String testCaseId = testItem.getTag().substring(matcher.start(), matcher.end() - 1);
            info.setTestCaseId(testCaseId);
        }
        else {
            info.setTestCaseId(testItem.getTag());
        }
        info.setComments(testItem.getTag());

        testItem.setTestCaseInformation(info);

        return configuration.getSerializer().writeValueAsBytes(testItem);
    }

    protected Path getSaveFilePath(TestItem<TInput, TExpected> testItem, TaskMessage message) {
        String baseFileName = testItem.getTag() == null ? message.getTaskId() : testItem.getTag();
        baseFileName = baseFileName + ".testcase";
        Path filePath = Paths.get(outputFolder, baseFileName);
        return filePath;
    }

    protected abstract byte[] getOutputContent(TResult result, TaskMessage message, TestItem<TInput, TExpected> testItem) throws Exception;

    protected abstract byte[] getFailedOutputContent(TaskMessage message, TestItem<TInput, TExpected> testItem) throws Exception;
}
