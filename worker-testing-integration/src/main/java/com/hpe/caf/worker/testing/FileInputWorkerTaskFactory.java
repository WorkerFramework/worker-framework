package com.hpe.caf.worker.testing;

import com.google.common.base.Strings;
import com.hpe.caf.util.ref.ReferencedData;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Created by ploch on 19/11/2015.
 */
public abstract class FileInputWorkerTaskFactory<TTask, TInput extends FileTestInputData, TExpected> implements WorkerTaskFactory<TTask, TInput, TExpected> {

    private final WorkerServices workerServices;
    private final String containerId;
    private final String testFilesFolder;
    private final String testSourcefileBaseFolder;
    private TestConfiguration testConfiguration;

    public FileInputWorkerTaskFactory(TestConfiguration configuration) throws Exception {
        this.workerServices = WorkerServices.getDefault();
        this.containerId = configuration.getDataStoreContainerId();
        this.testFilesFolder = configuration.getTestDataFolder();
        this.testSourcefileBaseFolder = configuration.getTestSourcefileBaseFolder();
        this.testConfiguration = Objects.requireNonNull(configuration);
    }

    @Override
    public TTask createTask(TestItem<TInput, TExpected> testItem) throws Exception {

        ReferencedData sourceData;
        if(!Strings.isNullOrEmpty(testConfiguration.getOverrideReference())){
            testItem.getInputData().setStorageReference(testConfiguration.getOverrideReference());
            sourceData = ReferencedData.getReferencedData(testConfiguration.getOverrideReference());
        }
        else if(!Strings.isNullOrEmpty(testItem.getInputData().getStorageReference())){
            sourceData = ReferencedData.getReferencedData(testItem.getInputData().getStorageReference());
        }
        else {
            Path inputFile = Paths.get(testItem.getInputData().getInputFile());

            if (Files.notExists(inputFile) && !Strings.isNullOrEmpty(testSourcefileBaseFolder)) {
                inputFile = Paths.get(testSourcefileBaseFolder, testItem.getInputData().getInputFile());
            }

            if (Files.notExists(inputFile)) {
                inputFile = Paths.get(testFilesFolder, testItem.getInputData().getInputFile());
            }


            if (testItem.getInputData().isUseDataStore()) {

                InputStream inputStream = Files.newInputStream(inputFile);
                String reference = workerServices.getDataStore().store(inputStream, containerId);
                sourceData = ReferencedData.getReferencedData(reference);
            } else {
                byte[] fileContent = Files.readAllBytes(inputFile);
                sourceData = ReferencedData.getWrappedData(fileContent);
            }
        }
        return createTask(testItem, sourceData);
    }

    protected abstract TTask createTask(TestItem<TInput, TExpected> testItem, ReferencedData sourceData);

    /**
     * Getter for property 'containerId'.
     *
     * @return Value for property 'containerId'.
     */
    protected String getContainerId() {
        return containerId;
    }
}
