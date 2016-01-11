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
import com.hpe.caf.storage.common.crypto.WrappedKey;
import com.hpe.caf.storage.common.model.AssetStatus;
import com.hpe.caf.storage.sdk.StorageClient;
import com.hpe.caf.storage.sdk.exceptions.StorageClientException;
import com.hpe.caf.storage.sdk.exceptions.StorageServiceConnectException;
import com.hpe.caf.storage.sdk.exceptions.StorageServiceException;
import com.hpe.caf.storage.sdk.model.AssetMetadata;
import com.hpe.caf.storage.sdk.model.requests.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * ManagedDataStore implementation for the CAF Storage Service.
 */
public class StorageServiceDataStore implements ManagedDataStore
{
    @FunctionalInterface
    private interface StorageClientFunction<T, R> {
        R apply(T t) throws StorageServiceConnectException, StorageServiceException, StorageClientException, IOException;
    }

    /**
     * The Storage Service "file type" for Worker assets.
     */
    private final AtomicInteger errors = new AtomicInteger(0);
    private final AtomicInteger numRx = new AtomicInteger(0);
    private final AtomicInteger numTx = new AtomicInteger(0);
    private final DataStoreMetricsReporter metrics = new StorageServiceDataStoreMetricsReporter();
    private final StorageClient storageClient;
    private String accessToken = null;
    private static final Logger LOG = LoggerFactory.getLogger(StorageServiceDataStore.class);
    private final KeycloakClient keycloakClient;
    /**
     * Byte size at which incoming streams are buffered to disk before sending to the Storage Service.
     */
    private static final int FILE_THRESHOLD = 1024 * 1024;


    public StorageServiceDataStore(final StorageServiceDataStoreConfiguration storageServiceDataStoreConfiguration)
    {
        storageClient = new StorageClient(storageServiceDataStoreConfiguration.getServerName(),
                String.valueOf(storageServiceDataStoreConfiguration.getPort()));

        keycloakClient = storageServiceDataStoreConfiguration.getAuthenticationConfiguration() != null ? new KeycloakClient(storageServiceDataStoreConfiguration.getAuthenticationConfiguration()) : null;
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

    private <T> T  callStorageService(StorageClientFunction<StorageClient, T> call)
            throws StorageServiceConnectException, IOException, StorageServiceException, StorageClientException {
        return callStorageService(call, 1);
    }

    private <T> T  callStorageService(StorageClientFunction<StorageClient, T> call, int retryCount)
            throws StorageServiceConnectException, StorageClientException, IOException, StorageServiceException {
        for (int i = 1; ; i++) {
            try {
                if (accessToken == null && keycloakClient != null) {
                    accessToken = keycloakClient.getAccessToken();
                }
                return call.apply(storageClient);
            }
            catch (StorageServiceException e) {
                if (i >= retryCount || e.getHTTPStatus() != 401 || keycloakClient == null) {
                    throw e;
                }
                accessToken = keycloakClient.getAccessToken();
            }
        }
    }

    @Override
    public InputStream retrieve(String reference)
        throws DataStoreException
    {
        LOG.debug("Received retrieve request for {}", reference);
        numRx.incrementAndGet();
        CafStoreReference ref = new CafStoreReference(reference);
        try {

            AssetMetadata assetMetadata = callStorageService(c -> c.getAssetMetadata(new GetAssetMetadataRequest(accessToken, ref.getContainer(), ref.getAsset())));
            if ( AssetStatus.ACTIVE.equals(AssetStatus.valueOf(assetMetadata.getStatus())) ) {
                WrappedKey wrappedKey = callStorageService(c -> c.getAssetContainerEncryptionKey(new GetAssetContainerEncryptionKeyRequest(accessToken, ref.getContainer())));
                return callStorageService(c -> c.downloadAsset(new DownloadAssetRequest(accessToken, ref.getContainer(), ref.getAsset(), wrappedKey))).getDecryptedStream();
            } else {
                errors.incrementAndGet();
                throw new DataStoreException(String.format("Reference %s is not active.", reference));
            }
        } catch (StorageClientException | StorageServiceException | StorageServiceConnectException | IOException e) {
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
            return callStorageService(c -> c.getAssetMetadata(new GetAssetMetadataRequest(accessToken, ref.getContainer(), ref.getAsset()))).getSize();
        } catch (IOException | StorageClientException |  StorageServiceException | StorageServiceConnectException e) {
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
            callStorageService(c -> c.listAssetContainers(new ListAssetContainersRequest(accessToken)));
        } catch (StorageServiceException e) {
            LOG.warn("Health check failed", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Error from Storage service: " + e.getResponseErrorMessage());
        } catch (StorageServiceConnectException | StorageClientException e) {
            LOG.warn("Health check failed", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Failed to connect to Storage service");
        } catch (IOException e) {
            LOG.warn("Health check failed", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Failed to request access token: " + e.getMessage());
        }
        return HealthResult.RESULT_HEALTHY;
    }

    private String store(ByteSource byteSource, String partialReference)
        throws DataStoreException
    {
        LOG.debug("Received store request for {}", partialReference);
        numTx.incrementAndGet();
        try (InputStream inputStream = byteSource.openBufferedStream()) {
            WrappedKey wrappedKey = callStorageService(c -> c.getAssetContainerEncryptionKey(new GetAssetContainerEncryptionKeyRequest(accessToken, partialReference)));
            AssetMetadata assetMetadata =
                    callStorageService(c -> c.uploadAsset(new UploadAssetRequest(accessToken, partialReference, UUID.randomUUID().toString(), wrappedKey, inputStream)));
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
