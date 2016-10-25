package com.hpe.caf.worker.testing.preparation;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.worker.testing.*;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by ploch on 25/11/2015.
 */
public class PreparationResultProcessor<TWorkerTask, TWorkerResult, TInput extends FileTestInputData, TExpected> extends OutputToFileProcessor<TWorkerResult, TInput, TExpected> {


    private final TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpected> configuration;

    protected PreparationResultProcessor(final TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpected> configuration, Codec codec) {
        super(codec, configuration.getWorkerResultClass(), configuration.getTestDataFolder());
        this.configuration = configuration;
    }

    /**
     * Getter for property 'configuration'.
     *
     * @return Value for property 'configuration'.
     */
    protected TestConfiguration<TWorkerTask, TWorkerResult, TInput, TExpected> getConfiguration() {
        return configuration;
    }

    @Override
    protected byte[] getOutputContent(TWorkerResult workerResult, TaskMessage message, TestItem<TInput, TExpected> testItem) throws Exception {

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

    @Override
    protected Path getSaveFilePath(TestItem<TInput, TExpected> testItem, TaskMessage message) {
        Path saveFilePath = super.getSaveFilePath(testItem, message);
        if (configuration.isStoreTestCaseWithInput()) {
            Path fileName = saveFilePath.getFileName();
            Path path = Paths.get(testItem.getInputData().getInputFile());
            saveFilePath = Paths.get(configuration.getTestDataFolder(), path.getParent() == null ? "" : path.getParent().toString(), fileName.toString());
        }
        return saveFilePath;
    }


    protected Path saveContentFile(TestItem<TInput, TExpected> testItem, String baseFileName, String extension, InputStream dataStream) throws IOException {
        String outputFolder = getOutputFolder();
        if (configuration.isStoreTestCaseWithInput()) {

            Path path = Paths.get(testItem.getInputData().getInputFile()).getParent();
            outputFolder =  Paths.get(configuration.getTestDataFolder(), path == null ? "" : path.toString()).toString();
        }

        baseFileName = FilenameUtils.normalize(baseFileName);
        baseFileName = Paths.get(baseFileName).getFileName().toString();
        Path contentFile = Paths.get(outputFolder, baseFileName + "." + extension + ".content");
        Files.deleteIfExists(contentFile);
        Files.copy(dataStream, contentFile, REPLACE_EXISTING);

        return getRelativeLocation(contentFile);
    }

    protected Path getRelativeLocation(Path contentFile) {
        Path relative = Paths.get(configuration.getTestDataFolder()).relativize(contentFile);
        return relative;
    }
}
