package com.hpe.caf.worker.testing;

import com.hpe.caf.util.ref.ReferencedData;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by ploch on 19/11/2015.
 */
public abstract class FileInputWorkerTaskFactory<TTask, TInput extends FileTestInputData, TExpected> implements WorkerTaskFactory<TTask, TInput, TExpected> {

    private final WorkerServices workerServices;
    private final String containerId;
    private final String testFilesFolder;

    public FileInputWorkerTaskFactory(TestConfiguration configuration) throws Exception {
        this(WorkerServices.getDefault(), configuration.getDataStoreContainerId(), configuration.getTestDataFolder() );
    }

    public FileInputWorkerTaskFactory(WorkerServices workerServices, String containerId, String testFilesFolder) {

        this.workerServices = workerServices;
        this.containerId = containerId;
        this.testFilesFolder = testFilesFolder;
    }

    @Override
    public TTask createTask(TestItem<TInput, TExpected> testItem) throws Exception {
        Path inputFile = Paths.get(testItem.getInputData().getInputFile());
        if (Files.notExists(inputFile)) {
            inputFile = Paths.get(testFilesFolder, testItem.getInputData().getInputFile());
        }

        ReferencedData sourceData;
        if (testItem.getInputData().isUseDataStore()) {

            InputStream inputStream = Files.newInputStream(inputFile);
            String reference = workerServices.getDataStore().store(inputStream, containerId);
            sourceData = ReferencedData.getReferencedData(reference);
        } else {
            byte[] fileContent = Files.readAllBytes(inputFile);
            sourceData = ReferencedData.getWrappedData(fileContent);
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
