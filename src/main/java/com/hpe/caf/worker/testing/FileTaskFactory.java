package com.hpe.caf.worker.testing;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.testing.TaskFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Created by ploch on 29/10/2015.
 */
public abstract class FileTaskFactory<T> implements TaskFactory<String> {

    private final Codec codec = new JsonCodec();
    private final String CONTEXT_KEY = "context";
    private final byte[] CONTEXT_DATA = "testData".getBytes(StandardCharsets.UTF_8);
    //protected static final String CONTAINER_ID = "5e94c95bf5aa426e8de876e80fd34bed"; // caf storage container id
    //protected static final String CONTAINER_ID = null; //filesystem store - use null
    private final DataStore store;
    private final String containerId;
    private final String workerName;
    private final int apiVersion;

    protected FileTaskFactory(DataStore store, String containerId, String workerName, int apiVersion) {

        this.store = store;
        this.containerId = containerId;
        this.workerName = workerName;
        this.apiVersion = apiVersion;
    }

    private byte[] createSerializedTaskMessage(T task, String taskId) throws CodecException {
        Map<String, byte[]> context = Collections.singletonMap(CONTEXT_KEY, CONTEXT_DATA);
        TaskMessage msg = new TaskMessage(taskId, workerName, apiVersion, codec.serialise(task), TaskStatus.NEW_TASK, context);
        return codec.serialise(msg);
    }

    public byte[] createProduct(String taskId, String inputFileName) throws FileNotFoundException, DataStoreException, CodecException {
        T task = createTask(store, new FileInputStream(inputFileName));
        return createSerializedTaskMessage(task, taskId);
    }



    protected T createTask(DataStore store, FileInputStream fileInputStream) throws DataStoreException{
        String storeRef = store.store(fileInputStream, containerId);

        ReferencedData referencedData = ReferencedData.getReferencedData(storeRef);

        return getTask(containerId, referencedData);
    }

    protected abstract T getTask(String containerId, ReferencedData referencedData);

}
