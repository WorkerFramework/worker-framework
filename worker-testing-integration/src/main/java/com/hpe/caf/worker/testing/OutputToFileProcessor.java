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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.QueueTaskMessage;
import com.hpe.caf.api.worker.TaskMessage;

/**
 * Created by ploch on 08/11/2015.
 */
public abstract class OutputToFileProcessor<TResult, TInput, TExpected> extends AbstractResultProcessor<TResult, TInput, TExpected>
{
    private final String outputFolder;

    /**
     * Getter for property 'outputFolder'.
     *
     * @return Value for property 'outputFolder'.
     */
    protected String getOutputFolder()
    {
        return outputFolder;
    }

    protected OutputToFileProcessor(final Codec codec, final Class<TResult> resultClass, final String outputFolder)
    {
        super(codec, resultClass);
        this.outputFolder = outputFolder;
    }

    @Override
    protected boolean processWorkerResult(TestItem<TInput, TExpected> testItem, TaskMessage message, TResult result) throws Exception
    {
        byte[] content = getOutputContent(result, message, testItem);
        return processResult(testItem, message, content);
    }

    @Override
    protected boolean processWorkerResult(TestItem<TInput, TExpected> testItem, QueueTaskMessage message, TResult result) throws Exception
    {
        byte[] content = getOutputContent(result, message, testItem);
        return processResult(testItem, message, content);
    }

    public byte[] getSerializedTestItem(TestItem<TInput, TExpected> testItem, TestConfiguration configuration) throws Exception
    {
        TestCaseInfo info = new TestCaseInfo();
        Matcher matcher = Pattern.compile(".*[/\\\\]").matcher(testItem.getTag());
        if (matcher.find()) {
            String testCaseId = testItem.getTag().substring(matcher.start(), matcher.end() - 1);
            info.setTestCaseId(testCaseId);
        } else {
            info.setTestCaseId(testItem.getTag());
        }
        info.setComments(testItem.getTag());

        testItem.setTestCaseInformation(info);

        return configuration.getSerializer().writeValueAsBytes(testItem);
    }

    public boolean processResult(TestItem<TInput, TExpected> testItem, TaskMessage message, byte[] content) throws IOException
    {
        Path filePath = getSaveFilePath(testItem, message);
        Files.deleteIfExists(filePath);
        while (Files.exists(filePath)); // Wait till the file is really deleted.
        Files.write(filePath, content, StandardOpenOption.CREATE);
        return true;
    }

    public boolean processResult(TestItem<TInput, TExpected> testItem, QueueTaskMessage message, byte[] content) throws IOException
    {
        Path filePath = getSaveFilePath(testItem, message);
        Files.deleteIfExists(filePath);
        while (Files.exists(filePath)); // Wait till the file is really deleted.
        Files.write(filePath, content, StandardOpenOption.CREATE);
        return true;
    }

    protected Path getSaveFilePath(TestItem<TInput, TExpected> testItem, TaskMessage message)
    {
        String baseFileName = testItem.getTag() == null ? message.getTaskId() : testItem.getTag();
        baseFileName = baseFileName + ".testcase";
        Path filePath = Paths.get(outputFolder, baseFileName);
        return filePath;
    }

    protected Path getSaveFilePath(TestItem<TInput, TExpected> testItem, QueueTaskMessage message)
    {
        String baseFileName = testItem.getTag() == null ? message.getTaskId() : testItem.getTag();
        baseFileName = baseFileName + ".testcase";
        Path filePath = Paths.get(outputFolder, baseFileName);
        return filePath;
    }

    protected abstract byte[] getOutputContent(TResult result, TaskMessage message, TestItem<TInput, TExpected> testItem) throws Exception;
    protected abstract byte[] getOutputContent(TResult result, QueueTaskMessage message, TestItem<TInput, TExpected> testItem) throws Exception;
}
