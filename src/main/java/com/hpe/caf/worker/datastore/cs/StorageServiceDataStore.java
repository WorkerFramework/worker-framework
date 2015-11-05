package com.hpe.caf.worker.datastore.cs;


import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.FileBackedOutputStream;
import com.google.common.io.Files;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.DataStoreMetricsReporter;
import com.hpe.caf.api.worker.ManagedDataStore;
import com.hpe.caf.storage.common.crypto.CryptoKey;
import com.hpe.caf.storage.common.crypto.WrappedKey;
import com.hpe.caf.storage.common.model.AssetStatus;
import com.hpe.caf.storage.sdk.StorageClient;
import com.hpe.caf.storage.sdk.exceptions.StorageClientException;
import com.hpe.caf.storage.sdk.exceptions.StorageServiceConnectException;
import com.hpe.caf.storage.sdk.exceptions.StorageServiceException;
import com.hpe.caf.storage.sdk.model.AssetMetadata;
import com.hpe.caf.storage.sdk.util.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * ManagedDataStore implementation for the CAF Storage Service.
 */
public class StorageServiceDataStore implements ManagedDataStore
{
    /**
     * The Storage Service "file type" for Worker assets.
     */
    public static final String WORKER_ASSET_TYPE = "WorkerDataStoreAsset";
    private final AtomicInteger errors = new AtomicInteger(0);
    private final AtomicInteger numRx = new AtomicInteger(0);
    private final AtomicInteger numTx = new AtomicInteger(0);
    private final DataStoreMetricsReporter metrics = new StorageServiceDataStoreMetricsReporter();
    private final StorageClient storageClient;
    private final String accessToken;
    private static final Logger LOG = LoggerFactory.getLogger(StorageServiceDataStore.class);
    /**
     * Byte size at which incoming streams are buffered to disk before sending to the Storage Service.
     */
    private static final int FILE_THRESHOLD = 1024 * 1024;


    public StorageServiceDataStore(final StorageServiceDataStoreConfiguration storageServiceDataStoreConfiguration)
    {
        //TODO Authentication is still disabled
        accessToken = "1234";
        storageClient = new StorageClient(storageServiceDataStoreConfiguration.getServerName(),
                String.valueOf(storageServiceDataStoreConfiguration.getPort()));

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


    @Override
    public InputStream retrieve(String reference)
        throws DataStoreException
    {
        LOG.debug("Received retrieve request for {}", reference);
        numRx.incrementAndGet();
        CafStoreReference ref = new CafStoreReference(reference);
        try {
            AssetMetadata assetMetadata = storageClient.getAssetMetadata(accessToken, ref.getContainer(), ref.getAsset());
            if ( AssetStatus.ACTIVE.equals(AssetStatus.valueOf(assetMetadata.getStatus())) ) {
                WrappedKey wrappedKey = this.storageClient.getAssetContainerEncryptionKey(accessToken, ref.getContainer());
                return storageClient.downloadAsset(accessToken, ref.getContainer(), ref.getAsset(), wrappedKey).getDecryptedStream();
            } else {
                errors.incrementAndGet();
                throw new DataStoreException(String.format("Reference %s is not active.", reference));
            }
        } catch (StorageServiceException | StorageServiceConnectException | IOException e) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to retrieve data from reference " + reference, e);
        }
    }


    @Override
    public long size(String reference)
        throws DataStoreException
    {
        LOG.debug("Received size request for {}", reference);
        CafStoreReference ref = new CafStoreReference(reference);
        try {
            return storageClient.getAssetMetadata(accessToken, ref.getContainer(), ref.getAsset()).getSize();
        } catch (StorageServiceException | StorageServiceConnectException e) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to get data size for reference " + reference, e);
        }
    }


    @Override
    public String store(InputStream inputStream, String partialReference)
        throws DataStoreException
    {
        try ( FileBackedOutputStream fileBackedOutputStream = new FileBackedOutputStream(FILE_THRESHOLD, true) ) {
            try {
                ByteStreams.copy(inputStream, fileBackedOutputStream);
                return store(fileBackedOutputStream.asByteSource(), partialReference);
            } finally {
                fileBackedOutputStream.reset();
            }
        } catch (IOException ex) {
            errors.incrementAndGet();
            throw new DataStoreException("Could not store input stream.", ex);
        }
    }


    @Override
    public String store(byte[] bytes, String partialReference)
        throws DataStoreException
    {
        return store(ByteSource.wrap(bytes), partialReference);
    }


    @Override
    public String store(Path path, String partialReference)
        throws DataStoreException
    {
        return store(Files.asByteSource(path.toFile()), partialReference);
    }


    @Override
    public HealthResult healthCheck()
    {
        try {
            storageClient.listAssetContainers(accessToken);
        } catch (StorageServiceException e) {
            LOG.warn("Health check failed", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Error from Storage service: " + e.getResponseErrorMessage());
        } catch (StorageServiceConnectException e) {
            LOG.warn("Health check failed", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Failed to connect to Storage service");
        }
        return HealthResult.RESULT_HEALTHY;
    }


    private String store(ByteSource byteSource, String partialReference)
        throws DataStoreException
    {
        LOG.debug("Received store request for {}", partialReference);
        numTx.incrementAndGet();
        CryptoKey assetKey = EncryptionUtil.generateRandomKey();
        try (InputStream inputStream = byteSource.openBufferedStream()) {
            WrappedKey wrappedKey = storageClient.getAssetContainerEncryptionKey(accessToken, partialReference);
            AssetMetadata assetMetadata =
                storageClient.uploadAsset(accessToken, partialReference, wrappedKey, assetKey, inputStream, null, UUID.randomUUID().toString(),
                                          byteSource.size(), null, WORKER_ASSET_TYPE, new Date(), new Date(), null);
            return new CafStoreReference(assetMetadata.getContainerId(), assetMetadata.getAssetId()).toString();
        } catch (IOException e) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to open buffered stream.", e);
        } catch (StorageClientException | StorageServiceException | StorageServiceConnectException e) {
            throw new DataStoreException("Failed to store data", e);
        }
    }


    private class StorageServiceDataStoreMetricsReporter implements DataStoreMetricsReporter
    {
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
