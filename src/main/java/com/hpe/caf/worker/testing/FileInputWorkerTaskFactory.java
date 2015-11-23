package com.hpe.caf.worker.testing;

import com.hpe.caf.util.ref.ReferencedData;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by ploch on 19/11/2015.
 */
public abstract class FileInputWorkerTaskFactory<TTask, TInput extends FileTestInputData, TExpected> implements WorkerTaskFactory<TTask, TInput, TExpected> {

    private final WorkerServices workerServices;
    private final String containerId;

    public FileInputWorkerTaskFactory() throws Exception {
        this(WorkerServices.getDefault(), SettingsProvider.defaultProvider.getSetting(SettingNames.dataStoreContainerId) );
    }

    public FileInputWorkerTaskFactory(WorkerServices workerServices, String containerId) {

        this.workerServices = workerServices;
        this.containerId = containerId;
    }

    @Override
    public TTask createTask(TestItem<TInput, TExpected> testItem) throws Exception {
        byte[] fileContent = Files.readAllBytes(Paths.get(testItem.getInputData().getInputFile()));

        ReferencedData sourceData;
        if (testItem.getInputData().isUseDataStore()) {

            String reference = workerServices.getDataStore().store(fileContent, containerId);
            sourceData = ReferencedData.getReferencedData(reference);
        } else {
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
