package com.hpe.caf.worker.example;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.util.ref.DataSource;
import com.hpe.caf.util.ref.DataSourceException;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.AbstractWorker;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Exemplar worker. This is the class responsible for processing the text data by the action specified in the task.
 */
public class ExampleWorker extends AbstractWorker<ExampleWorkerTask, ExampleWorkerResult> {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleWorker.class);
    private final DataStore dataStore;
    private final long resultSizeThreshold;

    public ExampleWorker(final ExampleWorkerTask task, final DataStore dataStore, final String outputQueue, final Codec codec, final long resultSizeThreshold) throws InvalidTaskException {
        super(task, outputQueue, codec);
        this.dataStore = Objects.requireNonNull(dataStore);
        this.resultSizeThreshold = resultSizeThreshold;
    }

    public String getWorkerIdentifier() {
        return ExampleWorkerConstants.WORKER_NAME;
    }

    public int getWorkerApiVersion() {
        return ExampleWorkerConstants.WORKER_API_VER;
    }

    /**
     * Trigger processing of the source file and determine a response.
     * @return  a response from the operation
     * @throws InterruptedException if the task is interrupted
     * @throws TaskRejectedException
     */
    @Override
    public WorkerResponse doWork() throws InterruptedException, TaskRejectedException {
        ExampleWorkerResult result = processFile();
        if(result.getWorkerStatus() == ExampleWorkerStatus.COMPLETED){
            return createSuccessResult(result);
        } else {
            return createFailureResult(result);
        }
    }

    private ExampleWorkerResult processFile() throws InterruptedException {
        LOG.info("Starting work");
        checkIfInterrupted();

        DataSource source = new DataStoreSource(dataStore, getCodec());
        ReferencedData data = getTask().getSourceData();
        try {
            InputStream textStream = data.acquire(source);

            String original = IOUtils.toString(textStream, StandardCharsets.UTF_8);
            String result = "";

            /** Simple way to choose which action should be used to process the text. **/
            if(getTask().getAction().equals("reverse")){
                for(int i=original.length()-1; i>=0; i--){
                    result = result + original.charAt(i);
                }
            } else if(getTask().getAction().equals("capitalise")){
                result = original.toUpperCase();
            } else if(getTask().getAction().equals("verbatim")){
                result = original;
            }

            /**write to datastore using the wrap method below**/
            ReferencedData textDataSource = wrapAsReferencedData(result.getBytes());

            /**create the worker result with the result text and COMPLETED worker status.**/
            ExampleWorkerResult workerResult = new ExampleWorkerResult();
            workerResult.setWorkerStatus(ExampleWorkerStatus.COMPLETED);
            workerResult.setTextData(textDataSource);

            return workerResult;
        } catch(DataSourceException e) {
            LOG.warn("Error acquiring data", e);
            return createErrorResult(ExampleWorkerStatus.SOURCE_FAILED);
        } catch (DataStoreException e) {
            LOG.warn("Error storing result", e);
            return createErrorResult(ExampleWorkerStatus.STORE_FAILED);
        } catch (IOException e) {
            LOG.warn("Error converting input stream to text", e);
            return createErrorResult(ExampleWorkerStatus.WORKER_EXAMPLE_FAILED);
        }
    }

    private ExampleWorkerResult createErrorResult(ExampleWorkerStatus status){
        ExampleWorkerResult workerResult = new ExampleWorkerResult();
        workerResult.setWorkerStatus(status);
        return workerResult;
    }

    private ReferencedData wrapAsReferencedData(final byte[] data) throws DataSourceException, DataStoreException {
        ReferencedData refData;
        if (data.length > resultSizeThreshold) {
            /** Wrap as datastore reference. **/
            String ref = dataStore.store(new ByteArrayInputStream(data), getTask().getDatastorePartialReference());
            refData = ReferencedData.getReferencedData(ref);
        } else {
            /**Wrap as byte array. **/
            refData = ReferencedData.getWrappedData(data);
        }
        return refData;
    }

}
