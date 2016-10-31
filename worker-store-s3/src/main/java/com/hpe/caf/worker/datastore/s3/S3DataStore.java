package com.hpe.caf.worker.datastore.s3;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.devicefarm.model.ArgumentException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.util.StringUtils;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.QuietResource;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.DataStoreMetricsReporter;
import com.hpe.caf.api.worker.ManagedDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * ManagedDataStore implementation for Amazon S3.
 */
public class S3DataStore implements ManagedDataStore
{
    private final AtomicInteger errors = new AtomicInteger(0);
    private final AtomicInteger numRx = new AtomicInteger(0);
    private final AtomicInteger numTx = new AtomicInteger(0);
    private final AtomicInteger numDx = new AtomicInteger(0);

    private final DataStoreMetricsReporter metrics = new S3DataStoreMetricsReporter();
    private static final Logger LOG = LoggerFactory.getLogger(S3DataStore.class);

    private AmazonS3Client amazonS3Client = null;
    private String bucketName = null;

    public S3DataStore(final S3DataStoreConfiguration s3DataStoreConfiguration)
    {
        if(s3DataStoreConfiguration==null){
            throw new ArgumentException("s3DataStoreConfiguration was null.");
        }

        ClientConfiguration clientCfg = new ClientConfiguration();

        if(!StringUtils.isNullOrEmpty(s3DataStoreConfiguration.getProxyHost())){
            clientCfg.setProtocol(Protocol.valueOf(s3DataStoreConfiguration.getProxyProtocol()));
            clientCfg.setProxyHost(s3DataStoreConfiguration.getProxyHost());
            clientCfg.setProxyPort(s3DataStoreConfiguration.getProxyPort());
        }
        AWSCredentials credentials = new BasicAWSCredentials(s3DataStoreConfiguration.getAccessKey(), s3DataStoreConfiguration.getSecretKey());
        bucketName = s3DataStoreConfiguration.getBucketName();
        amazonS3Client = new AmazonS3Client(credentials, clientCfg);
        amazonS3Client.setBucketAccelerateConfiguration(new SetBucketAccelerateConfigurationRequest(bucketName,
                new BucketAccelerateConfiguration(BucketAccelerateStatus.Enabled)));
    }

    @Override
    public DataStoreMetricsReporter getMetrics()
    {
        return metrics;
    }


    @Override
    public void shutdown()
    {
        // nothing to do
    }

    public void delete(String reference) throws DataStoreException {
        LOG.debug("Received delete request for {}", reference);
        numDx.incrementAndGet();
        try {
            amazonS3Client.deleteObject(bucketName, reference);
        } catch (Exception e) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to delete asset data for reference " + reference, e);
        }
    }

    @Override
    public InputStream retrieve(String reference)
        throws DataStoreException
    {
        LOG.debug("Received retrieve request for {}", reference);
        numRx.incrementAndGet();
        try {
            S3Object s3Object = amazonS3Client.getObject(bucketName, reference);

            //Do not close this as we return the stream and it should be closed by the caller.
            return s3Object.getObjectContent();

        } catch (Exception e) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to retrieve data from reference " + reference, e);
        }
    }

    @Override
    public long size(String reference)
        throws DataStoreException
    {
        LOG.debug("Received size request for {}", reference);

        try (QuietResource<S3Object> s3Object = new QuietResource<>(amazonS3Client.getObject(bucketName, reference))){
            ObjectMetadata objectMetadata = s3Object.get().getObjectMetadata();
            return objectMetadata.getContentLength();
        } catch (Exception e) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to get data size for reference " + reference, e);
        }
    }

    @Override
    public String store(InputStream inputStream, String partialReference)
            throws DataStoreException
    {
        return store(inputStream, partialReference, null);
    }

    private String store(InputStream inputStream, String partialReference, Long length)
        throws DataStoreException
    {
        try {
            String fullReference = partialReference + UUID.randomUUID().toString();

            ObjectMetadata objectMetadata = new ObjectMetadata();
            if(length!=null){
                objectMetadata.setContentLength(length);
            }

            TransferManager transferManager = new TransferManager(amazonS3Client);
            Upload upload = transferManager.upload(bucketName, fullReference, inputStream, objectMetadata);

            upload.waitForCompletion();
//            amazonS3Client.putObject(bucketName, fullReference, inputStream, objectMetadata);

            transferManager.shutdownNow(false);
            return fullReference;
        } catch (Exception ex) {
            errors.incrementAndGet();
            throw new DataStoreException("Could not store input stream.", ex);
        }
    }

    @Override
    public String store(byte[] bytes, String partialReference)
        throws DataStoreException
    {
        try(QuietResource<ByteArrayInputStream> inputStream = new QuietResource<>(new ByteArrayInputStream(bytes)))
        {
            return store(inputStream.get(), partialReference, (long) bytes.length);
        }
        catch(Exception ex){
            errors.incrementAndGet();
            throw new DataStoreException("Could not create input stream.", ex);
        }
    }


    @Override
    public String store(Path path, String partialReference)
        throws DataStoreException
    {
        try(QuietResource<FileInputStream> inputStream = new QuietResource<>(new FileInputStream(path.toFile())))
        {
            return store(inputStream.get(), partialReference, path.toFile().length());
        }
        catch(IOException ex){
            errors.incrementAndGet();
            throw new DataStoreException(String.format("Could not create file input stream from %s.", path.toString()), ex);
        }
    }

    @Override
    public HealthResult healthCheck()
    {
        try {
            LOG.debug("Received healthcheck request for S3.");

            if (!amazonS3Client.doesBucketExist(bucketName))
            {
                return new HealthResult(HealthStatus.UNHEALTHY, "S3 bucket " + bucketName + " does not exist.");
            }
        } catch (Exception e) {
            LOG.warn("Health check failed", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Error from S3: " + e.getMessage());
        }
        return HealthResult.RESULT_HEALTHY;
    }

    private class S3DataStoreMetricsReporter implements DataStoreMetricsReporter
    {
        @Override
        public int getDeleteRequests() {
            return numDx.get();
        }

        @Override
        public int getStoreRequests()
        {
            return numTx.get();
        }

        @Override
        public int getRetrieveRequests()
        {
            return numRx.get();
        }

        @Override
        public int getErrors()
        {
            return errors.get();
        }
    }
}
