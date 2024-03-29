/*
 * Copyright 2015-2024 Open Text.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.worker.datastore.fs;

import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.worker.*;
import org.apache.commons.io.output.ProxyOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * This is a simple DataStore that reads and writes files to and from a directory upon the file system. The store directory must be an
 * absolute path.
 */
public class FileSystemDataStore implements ManagedDataStore, FilePathProvider, DataStoreOutputStreamSupport
{
    private Path dataStorePath;
    private final int outputBufferSize;
    private final AtomicInteger errors = new AtomicInteger(0);
    private final AtomicInteger numRx = new AtomicInteger(0);
    private final AtomicInteger numTx = new AtomicInteger(0);
    private final AtomicInteger numDx = new AtomicInteger(0);
    private final DataStoreMetricsReporter metrics = new FileSystemDataStoreMetricsReporter();
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemDataStore.class);
    private final Callable<HealthResult> healthcheck;
    private final Duration healthcheckTimeout;
    private final ExecutorService healthcheckExecutor;

    /**
     * Determine the directory for the data store, and create it if necessary.
     */
    public FileSystemDataStore(final FileSystemDataStoreConfiguration config)
        throws DataStoreException
    {
        dataStorePath = FileSystems.getDefault().getPath(config.getDataDir());
        if (!doesPathExist(dataStorePath)) {
            try {
                Files.createDirectory(dataStorePath);
            } catch (IOException e) {
                throw new DataStoreException("Cannot create data store directory", e);
            }
        }
        healthcheck = new FileSystemDataStoreHealthcheck(dataStorePath);
        healthcheckTimeout = getDataDirHealthcheckTimeoutSeconds(config);
        healthcheckExecutor = Executors.newSingleThreadExecutor();
        outputBufferSize = getOutputBufferSize(config);

        LOG.debug("Initialised");
    }

    @Override
    public void shutdown()
    {
        healthcheckExecutor.shutdown();
    }

    /**
     * Delete a file.
     *
     * @param reference the file to be deleted.
     * @throws DataStoreException if the reference cannot be accessed or deleted
     */
    @Override
    public void delete(String reference)
        throws DataStoreException
    {
        Objects.requireNonNull(reference);
        try {
            numDx.incrementAndGet();
            LOG.debug("Deleting {}", reference);
            Path path = FileSystems.getDefault().getPath(dataStorePath.toString(), reference);
            Files.delete(path);
        } catch (IOException | SecurityException | InvalidPathException e) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to delete reference", e);
        }
    }

    /**
     * {@inheritDoc} Read a file from disk in the data directory.
     *
     * @throws ReferenceNotFoundException if the requested reference does not exist in the file system
     * @throws DataStoreException if the reference is found but cannot be accessed or retrieved
     * @throws InvalidPathException if the reference cannot be converted to a Path
     */
    @Override
    public InputStream retrieve(final String reference)
        throws DataStoreException
    {
        Objects.requireNonNull(reference);
        try {
            numRx.incrementAndGet();
            LOG.debug("Requesting {}", reference);
            return Files.newInputStream(checkReferenceExists(verifyReference(reference)));
        } catch (IOException e) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to retrieve data", e);
        }
    }

    /**
     * @throws ReferenceNotFoundException if the requested reference does not exist in the file system
     * @throws DataStoreException if the reference is found but cannot be accessed or retrieved
     * @throws InvalidPathException if the reference cannot be converted to a Path
     */
    @Override
    public long size(final String reference)
        throws DataStoreException
    {
        Objects.requireNonNull(reference);
        try {
            return Files.size(checkReferenceExists(verifyReference(reference)));
        } catch (IOException e) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to get data size", e);
        }
    }

    @Override
    public DataStoreMetricsReporter getMetrics()
    {
        return metrics;
    }

    /**
     * @throws DataStoreException if the reference is found but cannot be accessed or retrieved
     * @throws InvalidPathException if the reference cannot be converted to a Path
     */
    @Override
    public String store(final InputStream dataStream, final String partialReference)
        throws DataStoreException
    {
        try {
            Path ref = getStoreReference(partialReference);
            Files.copy(dataStream, ref);
            return dataStorePath.relativize(ref).toString().replace('\\', '/');
        } catch (IOException e) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to get output stream for store", e);
        }
    }

    /**
     * @throws DataStoreException if the reference is found but cannot be accessed or retrieved
     * @throws InvalidPathException if the reference cannot be converted to a Path
     */
    @Override
    public String store(byte[] data, String partialReference)
        throws DataStoreException
    {
        try {
            Path ref = getStoreReference(partialReference);
            Files.write(ref, data);
            return dataStorePath.relativize(ref).toString().replace('\\', '/');
        } catch (IOException e) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to get output stream for store", e);
        }
    }

    /**
     * @throws DataStoreException if the reference is found but cannot be accessed or retrieved
     * @throws InvalidPathException if the reference cannot be converted to a Path
     */
    @Override
    public String store(Path dataPath, String partialReference)
        throws DataStoreException
    {
        try {
            Path ref = getStoreReference(partialReference);
            Files.copy(dataPath, ref);
            return dataStorePath.relativize(ref).toString().replace('\\', '/');
        } catch (IOException e) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to get output stream for store", e);
        }
    }

    @Override
    public OutputStream store(final String partialReference, final Consumer<String> setReferenceFunction)
        throws DataStoreException
    {
        Objects.requireNonNull(setReferenceFunction);

        try {
            final Path ref = getStoreReference(partialReference);
            final String reference = dataStorePath.relativize(ref).toString().replace('\\', '/');
            final OutputStream fos = Files.newOutputStream(ref);
            final BufferedOutputStream bos = new BufferedOutputStream(fos, outputBufferSize);
            return new ProxyOutputStream(bos)
            {
                @Override
                public void close() throws IOException
                {
                    super.close();
                    setReferenceFunction.accept(reference);
                }
            };
        } catch (final IOException e) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to get output stream for store", e);
        }
    }

    @Override
    public HealthResult healthCheck()
    {
        final Future<HealthResult> healthcheckFuture = healthcheckExecutor.submit(healthcheck);
        try {
            return healthcheckFuture.get(healthcheckTimeout.getSeconds(), TimeUnit.SECONDS);
        } catch (final TimeoutException e) {
            healthcheckFuture.cancel(true);
            return new HealthResult(
                HealthStatus.UNHEALTHY,
                String.format("Timeout after %s seconds trying to access data store directory %s",
                              healthcheckTimeout.getSeconds(),
                              dataStorePath.toString()));
        } catch (final InterruptedException | ExecutionException e) {
            healthcheckFuture.cancel(true);
            LOG.warn("Exception thrown trying to access data store directory {} during healthcheck",
                     dataStorePath.toString(),
                     e);
            return new HealthResult(
                HealthStatus.UNHEALTHY,
                String.format("Exception thrown trying to access data store directory %s", dataStorePath.toString()));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws ReferenceNotFoundException if the requested reference does not exist in the file system
     * @throws DataStoreException if the reference is found but cannot be accessed or retrieved
     * @throws InvalidPathException if the reference cannot be converted to a Path
     */
    @Override
    public Path getFilePath(final String reference)
        throws DataStoreException
    {
        Objects.requireNonNull(reference);
        numRx.incrementAndGet();
        LOG.debug("Requesting file path for {}", reference);
        return checkReferenceExists(verifyReference(reference));
    }

    private boolean doesPathExist(final Path path) throws DataStoreException
    {
        try {
            path.getFileSystem().provider().checkAccess(path);
        } catch (final NoSuchFileException ex) {
            return false;
        } catch (final IOException ex) {
            errors.incrementAndGet();
            throw new DataStoreException("Path " + path + " is not accessible.", ex);
        }

        return true;
    }

    /**
     * The returned Path will be the partial reference resolved relative to the store's dataStorePath (from its configuration) and a
     * randomly generated UUID file name will be made relative to that. Subdirectories under the dataStorePath will automatically be
     * created.
     *
     * @param partialReference the partial reference, typically subdirectories, may be null
     * @return the absolute Path to the location to store data
     * @throws DataStoreException if the partial reference cannot be verified
     * @throws IOException if the subdirectories cannot be created
     */
    private Path getStoreReference(final String partialReference)
        throws DataStoreException, IOException
    {
        Path p;
        if (partialReference != null && !partialReference.isEmpty()) {
            p = verifyReference(validateReference(partialReference));
            if (!doesPathExist(p)) {
                Files.createDirectories(p);
            }
        } else {
            p = dataStorePath;
        }
        return p.resolve(UUID.randomUUID().toString());
    }

    /**
     * Prevents the use of data store references containing the backslash character.
     *
     * @param reference the data store reference to be validated
     * @return the supplied reference, if valid
     * @throws DataStoreException if the supplied reference is invalid
     */
    private String validateReference(final String reference)
        throws DataStoreException
    {
        if (reference.contains("\\")) {
            throw new DataStoreException("Invalid reference - contains the backslash character");
        }
        return reference;
    }

    /**
     * Prevent a caller trying to "break out" of the root dataStorePath by performing a full path resolution of the reference.
     *
     * @param reference the data store reference relative to dataStorePath to resolve
     * @return the resolved path, if valid
     * @throws DataStoreException if the reference is invalid or cannot be resolved
     * @throws InvalidPathException if the reference cannot be converted to a Path
     */
    private Path verifyReference(final String reference)
        throws DataStoreException
    {
        Path p = dataStorePath.resolve(reference).normalize();
        if (!p.startsWith(dataStorePath)) {
            throw new DataStoreException("Invalid reference");
        }
        return p;
    }

    /**
     * Take a Path, verify it exists. If it does, return it, else throw ReferenceNotFoundException.
     *
     * @param reference the reference Path to check
     * @return the Path, if it exists
     * @throws ReferenceNotFoundException if the Path does not exist
     */
    private Path checkReferenceExists(final Path reference)
        throws DataStoreException, ReferenceNotFoundException
    {
        if (!doesPathExist(reference)) {
            throw new ReferenceNotFoundException("Reference not found: " + reference);
        }
        return reference;
    }

    private class FileSystemDataStoreMetricsReporter implements DataStoreMetricsReporter
    {
        @Override
        public int getDeleteRequests()
        {
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

    private static int getOutputBufferSize(final FileSystemDataStoreConfiguration config)
    {
        final int DEFAULT_OUTPUT_BUFFER_SIZE = 16384;

        final Integer configSetting = config.getOutputBufferSize();

        return (configSetting == null || configSetting <= 0)
            ? DEFAULT_OUTPUT_BUFFER_SIZE
            : configSetting;
    }

    private static Duration getDataDirHealthcheckTimeoutSeconds(final FileSystemDataStoreConfiguration config)
    {
        final int DEFAULT_DATA_DIR_HEALTHCHECK_TIMEOUT_SECONDS = 10;

        final Integer configSetting = config.getDataDirHealthcheckTimeoutSeconds();

        return (configSetting == null || configSetting <= 0)
            ? Duration.ofSeconds(DEFAULT_DATA_DIR_HEALTHCHECK_TIMEOUT_SECONDS)
            : Duration.ofSeconds(configSetting);
    }
}
