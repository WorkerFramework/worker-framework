package com.hpe.caf.worker.datastore.cs;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.FileBackedOutputStream;
import com.google.common.io.Files;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.DataStoreMetricsReporter;
import com.hpe.caf.api.worker.ManagedDataStore;
import com.hpe.caf.storage.common.crypto.CryptoKey;
import com.hpe.caf.storage.common.crypto.WrappedKey;
import com.hpe.caf.storage.common.model.AssetStatus;
import com.hpe.caf.storage.sdk.StorageClient;
import com.hpe.caf.storage.sdk.model.AssetMetadata;
import com.hpe.caf.storage.sdk.util.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


public class StorageServiceDataStore implements ManagedDataStore
{
    private final AtomicInteger errors = new AtomicInteger(0);
    private final AtomicInteger numRx = new AtomicInteger(0);
    private final AtomicInteger numTx = new AtomicInteger(0);
    private final DataStoreMetricsReporter metrics = new StorageServiceDataStoreMetricsReporter();
    private final ObjectMapper mapper = new ObjectMapper();
    private final StorageClient storageClient;
    private static final Logger LOG = LoggerFactory.getLogger(StorageServiceDataStore.class);
    private static final int FILE_THRESHOLD = 1024 * 1024;


    public StorageServiceDataStore(final StorageServiceDataStoreConfiguration storageServiceDataStoreConfiguration)
    {
        storageClient = new StorageClient(storageServiceDataStoreConfiguration.getServerName(), String.valueOf(storageServiceDataStoreConfiguration.getPort()),
                              storageServiceDataStoreConfiguration.getEmailAddress());
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
        StorageServiceReference storageServiceReference = getStorageServiceReference(Objects.requireNonNull(reference));
        if ( AssetStatus.ACTIVE.equals(AssetStatus.valueOf(getAssetMetadata(storageServiceReference).getStatus())) ) {
            WrappedKey wrappedKey = this.storageClient.getAssetContainerEncryptionKey(storageServiceReference.getContainerId());
            try {
                return this.storageClient.downloadAsset(storageServiceReference.getContainerId(), storageServiceReference.getAssetId(), wrappedKey).getDecryptedStream();
            } catch (IOException e) {
                throw new DataStoreException(String.format("Could not download asset %s.", reference), e);
            }
        } else {
            throw new DataStoreException(String.format("Reference %s is not active.", reference));
        }
    }


    private StorageServiceReference getStorageServiceReference(String reference)
        throws DataStoreException
    {
        try {
            return mapper.readValue(reference, StorageServiceReference.class);
        } catch (IOException e) {
            throw new DataStoreException(String.format("Invalid reference (%s).", reference), e);
        }
    }


    private AssetMetadata getAssetMetadata(StorageServiceReference storageServiceReference)
    {
        return this.storageClient.getAssetMetadata(storageServiceReference.getContainerId(), storageServiceReference.getAssetId());
    }


    @Override
    public long size(String reference)
        throws DataStoreException
    {
        StorageServiceReference storageServiceReference = getStorageServiceReference(Objects.requireNonNull(reference));
        return getAssetMetadata(storageServiceReference).getSize();
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
        //TODO verify underlying storage service is healthy
        return HealthResult.RESULT_HEALTHY;
    }


    private String store(ByteSource byteSource, String partialReference)
        throws DataStoreException
    {
        Objects.requireNonNull(partialReference);
        StorageServiceReference storageServiceReference = getStorageServiceReference(partialReference);
        verifyPartialReference(storageServiceReference);
        CryptoKey assetKey = EncryptionUtil.generateRandomKey();
        WrappedKey wrappedKey = this.storageClient.getAssetContainerEncryptionKey(storageServiceReference.getContainerId());
        try (InputStream inputStream = byteSource.openBufferedStream()) {
            AssetMetadata assetMetadata =
                this.storageClient.uploadAsset(storageServiceReference.getContainerId(), wrappedKey, assetKey, inputStream, null,
                                               UUID.randomUUID().toString(), //name
                                               byteSource.size(), null, //Description
                                               "WorkerDataStoreAsset", //Filetype
                                               new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()), null);//Custom metadata
            StorageServiceReference completeReference = new StorageServiceReference();
            completeReference.setAssetId(assetMetadata.getAssetId());
            completeReference.setContainerId(assetMetadata.getContainerId());
            completeReference.setRevId(assetMetadata.getRevId());
            return mapper.writeValueAsString(completeReference);

        } catch (IOException e) {
            throw new DataStoreException("Failed to open buffered stream.", e);
        }
    }


    private void verifyPartialReference(StorageServiceReference storageServiceReference)
        throws DataStoreException
    {
        if ( storageServiceReference.getAssetId() != null ) {
            throw new DataStoreException("Invalid partial reference supplied, assetId should not be supplied.");
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
