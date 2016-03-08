#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

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
public class ${workerName} extends AbstractWorker<${workerName}Task, ${workerName}Result> {

    /**
     * Logger for logging purposes.
     */
    private static final Logger LOG = LoggerFactory.getLogger(${workerName}.class);

    /**
     * Datastore used to store the result/read the reference.
     */
    private final DataStore dataStore;

    /**
     * Minimum size of result which should be wrapped as a datastore reference.
     */
    private final long resultSizeThreshold;

    public ${workerName}(final ${workerName}Task task, final DataStore dataStore, final String outputQueue, final Codec codec, final long resultSizeThreshold) throws InvalidTaskException {
        super(task, outputQueue, codec);
        this.dataStore = Objects.requireNonNull(dataStore);
        this.resultSizeThreshold = resultSizeThreshold;
    }

    @Override
    public String getWorkerIdentifier() {
        return ${workerName}Constants.WORKER_NAME;
    }

    @Override
    public int getWorkerApiVersion() {
        return ${workerName}Constants.WORKER_API_VER;
    }

    /**
     * Trigger processing of the source file and determine a response.
     * @return WorkerResponse - a response from the operation.
     * @throws InterruptedException - if the task is interrupted.
     * @throws TaskRejectedException
     */
    @Override
    public WorkerResponse doWork() throws InterruptedException, TaskRejectedException {
        ${workerName}Result result = processFile();
        if(result.getWorkerStatus() == ${workerName}Status.COMPLETED){
            return createSuccessResult(result);
        } else {
            return createFailureResult(result);
        }
    }

    /**
     * Private method to process the ReferencedData.
     * @return ${workerName}Result
     * @throws InterruptedException
     */
    private ${workerName}Result processFile() throws InterruptedException {
        LOG.info("Starting work");
        checkIfInterrupted();

        //Creation of DataSource using dataStore from constructor and serialization codec
        DataSource source = new DataStoreSource(dataStore, getCodec());

        ReferencedData data = getTask().getSourceData();

        try {
            //Acquire the inputstream data from the referenced data in the datasource
            InputStream textStream = data.acquire(source);

            //convert inputstream to a string
            String original = IOUtils.toString(textStream, StandardCharsets.UTF_8);
            String result = "";

            //manipulate the text by the method depicted by the task action
            if(getTask().getAction() == ${workerName}Action.REVERSE){
                for(int i=original.length()-1; i>=0; i--){
                    result = result + original.charAt(i);
                }
            } else if(getTask().getAction() == ${workerName}Action.CAPITALISE){
                result = original.toUpperCase();
            } else if(getTask().getAction() == ${workerName}Action.VERBATIM){
                result = original;
            }

            //write to the datastore using the wrapAsReferencedData method below
            ReferencedData textDataSource = wrapAsReferencedData(result.getBytes());

            //create the worker result with the resultant referenced data text data, set worker status complete
            ${workerName}Result workerResult = new ${workerName}Result();
            workerResult.setWorkerStatus(${workerName}Status.COMPLETED);
            workerResult.setTextData(textDataSource);

            return workerResult;
        } catch(DataSourceException e) {
            //DataSourceException thrown when retrieving data from the datastore
            LOG.warn("Error acquiring data", e);
            return createErrorResult(${workerName}Status.SOURCE_FAILED);
        } catch (DataStoreException e) {
            //DataStoreException thrown when storing data in the datastore
            LOG.warn("Error storing result", e);
            return createErrorResult(${workerName}Status.STORE_FAILED);
        } catch (IOException e) {
            //IOException thrown if the conversion from InputStream to String fails
            LOG.warn("Error converting input stream to text", e);
            return createErrorResult(${workerName}Status.WORKER_FAILED);
        }
    }

    /**
     * If an error in the worker occurs, create a new ${workerName}Result with the corresponding worker failure status.
     */
    private ${workerName}Result createErrorResult(${workerName}Status status){
        ${workerName}Result workerResult = new ${workerName}Result();
        workerResult.setWorkerStatus(status);
        return workerResult;
    }

    /**
     * If the length of the data is greater than the result size threshold, store the data in the datastore. Otherwise,
     * wrap as a byte array.
     * @param data
     * @return ReferencedData
     * @throws DataSourceException
     * @throws DataStoreException
     */
    private ReferencedData wrapAsReferencedData(final byte[] data) throws DataSourceException, DataStoreException {
        ReferencedData refData;
        if (data.length > resultSizeThreshold) {
            // Wrap as datastore reference.
            String ref = dataStore.store(new ByteArrayInputStream(data), getTask().getDatastorePartialReference());
            refData = ReferencedData.getReferencedData(ref);
        } else {
            //Wrap as byte array.
            refData = ReferencedData.getWrappedData(data);
        }
        return refData;
    }
}
