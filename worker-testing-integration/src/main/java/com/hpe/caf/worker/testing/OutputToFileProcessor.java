package com.hpe.caf.worker.testing;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.TaskMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

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

    protected Path getSaveFilePath(TestItem<TInput, TExpected> testItem, TaskMessage message) {
        String baseFileName = testItem.getTag() == null ? message.getTaskId() : testItem.getTag();
        baseFileName = baseFileName + ".testcase";
        Path filePath = Paths.get(outputFolder, baseFileName);
        return filePath;
    }

    protected abstract byte[] getOutputContent(TResult result, TaskMessage message, TestItem<TInput, TExpected> testItem) throws Exception;

    protected abstract byte[] getFailedOutputContent(TaskMessage message, TestItem<TInput, TExpected> testItem) throws Exception;
}
