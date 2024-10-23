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
package com.github.workerframework.worker.datastores.http;

import com.github.cafapi.common.api.HealthResult;
import com.github.cafapi.common.api.HealthStatus;
import com.github.cafapi.common.api.QuietResource;
import com.github.workerframework.worker.api.DataStore;
import com.github.workerframework.worker.api.DataStoreException;
import com.github.workerframework.worker.api.DataStoreMetricsReporter;
import com.github.workerframework.worker.api.ManagedDataStore;
import com.github.workerframework.worker.api.ReferenceNotFoundException;
import com.google.common.io.ByteStreams;

import java.io.DataOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link DataStore} that reads and writes files to and from a HTTP server.
 */
public class HttpDataStore implements ManagedDataStore
{
    private static final Logger LOG = LoggerFactory.getLogger(HttpDataStore.class);
    private static final String OCTET_STREAM_MEDIA_TYPE = "application/octet-stream";
    private static final String X_WWW_FORM_URLENCODED_MEDIA_TYPE = "application/x-www-form-urlencoded";

    private final AtomicInteger numErrors = new AtomicInteger(0);
    private final AtomicInteger numRetrieveRequests = new AtomicInteger(0);
    private final AtomicInteger numStoreRequests = new AtomicInteger(0);
    private final AtomicInteger numDeleteRequests = new AtomicInteger(0);
    private final DataStoreMetricsReporter metrics = new HttpDataStoreMetricsReporter();

    private final String url;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    public HttpDataStore(final HttpDataStoreConfiguration config)
        throws DataStoreException
    {
        url = config.getUrl();
        connectTimeoutMillis = config.getConnectTimeoutMillis();
        readTimeoutMillis = config.getReadTimeoutMillis();
        LOG.debug("Initialised");
    }

    @Override
    public DataStoreMetricsReporter getMetrics()
    {
        return metrics;
    }

    @Override
    public void shutdown()
    {
        LOG.debug("Shutdown");
    }

    @Override
    public HealthResult healthCheck()
    {
        HttpURLConnection httpUrlConnection = null;
        try {
            httpUrlConnection = (HttpURLConnection) new URL(url).openConnection();
            httpUrlConnection.setConnectTimeout(connectTimeoutMillis);
            httpUrlConnection.setReadTimeout(readTimeoutMillis);
            httpUrlConnection.setRequestMethod("GET");
            final int responseCode = httpUrlConnection.getResponseCode();
            if (isSuccessfulResponseCode(responseCode)) {
                return HealthResult.RESULT_HEALTHY;
            } else {
                return new HealthResult(
                    HealthStatus.UNHEALTHY,
                    String.format("Unexpected response code: %s returned from url: %s. Response message: %s",
                                  responseCode, url, httpUrlConnection.getResponseMessage()));
            }
        } catch (final IOException e) {
            LOG.warn("Exception thrown trying to access url: {} during healthcheck", url, e);
            return new HealthResult(
                HealthStatus.UNHEALTHY,
                String.format("Exception thrown trying access url: %s during healthcheck", url));
        } finally {
            if (httpUrlConnection != null) {
                httpUrlConnection.disconnect();
            }
        }
    }

    @Override
    public void delete(final String reference)
        throws DataStoreException
    {
        LOG.debug("Received delete request for data with reference: {}", reference);
        Objects.requireNonNull(reference);
        HttpURLConnection httpUrlConnection = null;
        try {
            final URL urlWithReference = new URL(String.join("/", url, reference));
            httpUrlConnection = (HttpURLConnection) urlWithReference.openConnection();
            httpUrlConnection.setConnectTimeout(connectTimeoutMillis);
            httpUrlConnection.setReadTimeout(readTimeoutMillis);
            httpUrlConnection.setDoOutput(true);
            httpUrlConnection.setRequestProperty("Content-Type", X_WWW_FORM_URLENCODED_MEDIA_TYPE);
            httpUrlConnection.setRequestMethod("DELETE");
            final int responseCode = httpUrlConnection.getResponseCode();
            if (isSuccessfulResponseCode(responseCode)) {
                LOG.debug("Successfully deleted data with reference: {}", reference);
            } else {
                numErrors.incrementAndGet();
                throw new DataStoreException(String.format(
                    "Failed to delete data with reference: %s. Response code: %s. Response message: %s",
                    reference, httpUrlConnection.getResponseCode(), httpUrlConnection.getResponseMessage()));
            }
        } catch (final IOException e) {
            numErrors.incrementAndGet();
            throw new DataStoreException(String.format("Failed to delete data with reference: %s", reference), e);
        } finally {
            if (httpUrlConnection != null) {
                httpUrlConnection.disconnect();
            }
        }
    }

    @Override
    public InputStream retrieve(final String reference)
        throws DataStoreException, ReferenceNotFoundException
    {
        LOG.debug("Received retrieve request for data with reference: {}", reference);
        numRetrieveRequests.incrementAndGet();
        Objects.requireNonNull(reference);
        HttpURLConnection httpUrlConnection = null;
        try {
            final URL urlWithReference = new URL(String.join("/", url, reference));
            httpUrlConnection = (HttpURLConnection) urlWithReference.openConnection();
            httpUrlConnection.setConnectTimeout(connectTimeoutMillis);
            httpUrlConnection.setReadTimeout(readTimeoutMillis);
            httpUrlConnection.setRequestMethod("GET");
            final int responseCode = httpUrlConnection.getResponseCode();
            if (isSuccessfulResponseCode(responseCode)) {
                LOG.debug("Successfully retrieved data with reference: {}", reference);
                return  new HttpURLConnectionInputStream(httpUrlConnection, httpUrlConnection.getInputStream());
            } else if (responseCode == 404) {
                numErrors.incrementAndGet();
                throw new ReferenceNotFoundException(String.format("No data found with reference: %s", reference));
            } else {
                numErrors.incrementAndGet();
                throw new DataStoreException(String.format(
                    "Failed to retrieve data with reference: %s. Response code: %s. Response message: %s",
                    reference, httpUrlConnection.getResponseCode(), httpUrlConnection.getResponseMessage()));
            }
        } catch (final IOException e) {
            numErrors.incrementAndGet();
            throw new DataStoreException(String.format("Failed to retrieve data with reference: %s", reference), e);
        }
        // Don't close httpUrlConnection as that will close the returned input stream, which the client is responsible for closing.
    }

    @Override
    public long size(final String reference)
        throws DataStoreException
    {
        LOG.debug("Received size request for data with reference: {}", reference);
        Objects.requireNonNull(reference);
        HttpURLConnection httpUrlConnection = null;
        try {
            final URL urlWithReference = new URL(String.join("/", url, reference));
            httpUrlConnection = (HttpURLConnection) urlWithReference.openConnection();
            httpUrlConnection.setConnectTimeout(connectTimeoutMillis);
            httpUrlConnection.setReadTimeout(readTimeoutMillis);
            httpUrlConnection.setRequestMethod("GET");
            final int responseCode = httpUrlConnection.getResponseCode();
            if (isSuccessfulResponseCode(responseCode)) {
                LOG.debug("Successfully got size of data with reference: {}", reference);
                return httpUrlConnection.getContentLengthLong();
            } else {
                numErrors.incrementAndGet();
                throw new DataStoreException(String.format(
                    "Failed to get size data with reference: %s. Response code: %s. Response message: %s",
                    reference, httpUrlConnection.getResponseCode(), httpUrlConnection.getResponseMessage()));
            }
        } catch (final IOException e) {
            numErrors.incrementAndGet();
            throw new DataStoreException(String.format("Failed to get size of data with reference: %s", reference), e);
        } finally {
            if (httpUrlConnection != null) {
                httpUrlConnection.disconnect();
            }
        }
    }

    @Override
    public String store(byte[] data, String partialReference)
        throws DataStoreException
    {
        LOG.debug("Received store request for partial reference (may be null, which is allowed): {}", partialReference);
        numStoreRequests.incrementAndGet();
        final String reference = (partialReference != null && !partialReference.isEmpty())
            ? String.join("/", partialReference, UUID.randomUUID().toString())
            : UUID.randomUUID().toString();
        HttpURLConnection httpUrlConnection = null;
        DataOutputStream dataOutputStream = null;
        try {
            final URL urlWithReference = new URL(String.join("/", url, reference));
            httpUrlConnection = (HttpURLConnection) urlWithReference.openConnection();
            httpUrlConnection.setConnectTimeout(connectTimeoutMillis);
            httpUrlConnection.setReadTimeout(readTimeoutMillis);
            httpUrlConnection.setDoOutput(true);
            httpUrlConnection.setRequestMethod("PUT");
            httpUrlConnection.setRequestProperty("Content-Type", OCTET_STREAM_MEDIA_TYPE);
            dataOutputStream = new DataOutputStream(httpUrlConnection.getOutputStream());
            dataOutputStream.write(data, 0, data.length);
            final int responseCode = httpUrlConnection.getResponseCode();
            if (isSuccessfulResponseCode(responseCode)) {
                LOG.debug("Successfully stored data with reference: {}", reference);
                return reference;
            } else {
                numErrors.incrementAndGet();
                throw new DataStoreException(String.format(
                    "Failed to store data with reference: %s. Response code: %s. Response message: %s",
                    reference, httpUrlConnection.getResponseCode(), httpUrlConnection.getResponseMessage()));
            }
        } catch (final IOException e) {
            numErrors.incrementAndGet();
            throw new DataStoreException(String.format("Failed to store data with reference: %s", reference), e);
        } finally {
            if (dataOutputStream != null) {
                try {
                    dataOutputStream.flush();
                    dataOutputStream.close();
                } catch (final IOException e) {
                    LOG.warn("Unable to flush or close output stream", e);
                }
            }
            if (httpUrlConnection != null) {
                httpUrlConnection.disconnect();
            }
        }
    }

    @Override
    public String store(final InputStream inputStream, final String partialReference)
        throws DataStoreException
    {
        try {
            return store(ByteStreams.toByteArray(inputStream), partialReference);
        } catch (IOException e) {
            throw new DataStoreException(String.format(
                "Failed to store data with  partial reference (may be null, which is allowed): %s, "
                + "as unable to create byte array from input stream.", partialReference), e);
        }
    }

    @Override
    public String store(final Path path, final String partialReference)
        throws DataStoreException
    {
        try (QuietResource<FileInputStream> inputStream = new QuietResource<>(new FileInputStream(path.toFile()))) {
            return store(inputStream.get(), partialReference);
        } catch (final IOException ex) {
            numErrors.incrementAndGet();
            throw new DataStoreException(String.format(
                "Unable to store data withCould not create file input stream from path: %s.", path.toString()), ex);
        }
    }

    // A custom InputStream implementation that automatically closes the HttpURLConnection when the stream is closed.
    private final class HttpURLConnectionInputStream extends InputStream
    {
        private final HttpURLConnection httpUrlConnection;
        private final InputStream inputStream;

        public HttpURLConnectionInputStream(final HttpURLConnection httpUrlConnection, final InputStream inputStream) throws IOException
        {
            this.httpUrlConnection = httpUrlConnection;
            this.inputStream = inputStream;
        }

        @Override
        public int read() throws IOException
        {
            return inputStream.read();
        }

        @Override
        public void close() throws IOException
        {
            try {
                inputStream.close();
            } catch (final IOException e) {
                LOG.warn("Unable to close input stream", e);
            }
            httpUrlConnection.disconnect();
        }
    }

    private class HttpDataStoreMetricsReporter implements DataStoreMetricsReporter
    {
        @Override
        public int getDeleteRequests()
        {
            return numDeleteRequests.get();
        }

        @Override
        public int getStoreRequests()
        {
            return numStoreRequests.get();
        }

        @Override
        public int getRetrieveRequests()
        {
            return numRetrieveRequests.get();
        }

        @Override
        public int getErrors()
        {
            return numErrors.get();
        }
    }

    private static boolean isSuccessfulResponseCode(final int responseCode)
    {
        return responseCode >= 200 && responseCode <= 299;
    }
}
