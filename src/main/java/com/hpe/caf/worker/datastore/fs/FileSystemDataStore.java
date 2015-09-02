package com.hpe.caf.worker.datastore.fs;


import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.DataStoreMetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * This is a simple DataStore that reads and writes files to and from
 * a directory upon the file system. The store directory must be an
 * absolute path.
 */
public class FileSystemDataStore extends DataStore
{
    private Path dataStorePath;
    private final AtomicInteger errors = new AtomicInteger(0);
    private final AtomicInteger numRx = new AtomicInteger(0);
    private final AtomicInteger numTx = new AtomicInteger(0);
    private final AtomicLong bytesRx = new AtomicLong(0);
    private final AtomicLong bytesTx = new AtomicLong(0);
    private final DataStoreMetricsReporter metrics = new FileSystemDataStoreMetricsReporter();
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemDataStore.class);


    /**
     * {@inheritDoc}
     *
     * Determine the directory for the data store, and create it if necessary.
     */
    public FileSystemDataStore(final FileSystemDataStoreConfiguration config)
            throws DataStoreException
    {
        dataStorePath = FileSystems.getDefault().getPath(config.getDataDir());
        if ( !Files.exists(dataStorePath) ) {
            try {
                Files.createDirectory(dataStorePath);
            } catch (IOException e) {
                throw new DataStoreException("Cannot create data store directory", e);
            }
        }
        LOG.debug("Initialised");
    }


    @Override
    public void shutdown()
    {
        // nothing to do
    }


    @Override
    /**
     * Read a file from disk in the data directory.
     */
    public byte[] getData(final String reference)
            throws DataStoreException
    {
        Objects.requireNonNull(reference);
        try {
            numRx.incrementAndGet();
            LOG.debug("Requesting {}", reference);
            byte[] ret = Files.readAllBytes(dataStorePath.resolve(reference));
            bytesRx.addAndGet(ret.length);
            return ret;
        } catch (IOException e) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to retrieve data", e);
        }
    }


    @Override
    /**
     * Write a file to disk in the data directory.
     */
    public String putData(final String reference, final byte[] data)
            throws DataStoreException
    {
        Objects.requireNonNull(reference);
        Objects.requireNonNull(data);
        try {
            numTx.incrementAndGet();
            LOG.debug("Storing {}", reference);
            String ret = Files.write(dataStorePath.resolve(reference), data).toString();
            bytesTx.addAndGet(data.length);
            return ret;
        } catch (IOException e) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to store data", e);
        }
    }


    @Override
    public DataStoreMetricsReporter getMetrics()
    {
        return metrics;
    }


    @Override
    public HealthResult healthCheck()
    {
        return HealthResult.RESULT_HEALTHY;
    }


    private class FileSystemDataStoreMetricsReporter implements DataStoreMetricsReporter
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


        @Override
        public long getBytesStored()
        {
            return bytesTx.get();
        }


        @Override
        public long getBytesRetrieved()
        {
            return bytesRx.get();
        }
    }
}
