/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.datastore.http;

import com.google.common.io.ByteStreams;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.QuietResource;
import com.hpe.caf.api.worker.*;
//import org.apache.commons.io.output.ProxyOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A {@link DataStore} that reads and writes files to and from a HTTP server.
 */
public class HttpDataStore implements ManagedDataStore//, DataStoreOutputStreamSupport
{
    private static final Logger LOG = LoggerFactory.getLogger(HttpDataStore.class);
    private static final MediaType OCTET_STREAM_MEDIA_TYPE = MediaType.parse("application/octet-stream");

    private final AtomicInteger numErrors = new AtomicInteger(0);
    private final AtomicInteger numRetrieveRequests = new AtomicInteger(0);
    private final AtomicInteger numStoreRequests = new AtomicInteger(0);
    private final AtomicInteger numDeleteRequests = new AtomicInteger(0);
    private final DataStoreMetricsReporter metrics = new HttpDataStoreMetricsReporter();

    private String url;
    //   private final int outputBufferSize;
    private final OkHttpClient httpClient;
    private final OkHttpClient healthcheckHttpClient;

    public HttpDataStore(final HttpDataStoreConfiguration config)
        throws DataStoreException
    {
        url = config.getUrl();
        httpClient
            = new OkHttpClient().newBuilder().callTimeout(config.getHttpCallTimeoutSeconds(), TimeUnit.SECONDS).build();
        healthcheckHttpClient
            = new OkHttpClient().newBuilder().callTimeout(config.getHealthcheckHttpCallTimeoutSeconds(), TimeUnit.SECONDS).build();
        //   outputBufferSize = getOutputBufferSize(config);
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
        // Nothing to do
    }

    @Override
    public HealthResult healthCheck()
    {
        final Request request = new Request.Builder().url(url).build(); // TODO just HEAD request?
        try (final Response response = healthcheckHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return HealthResult.RESULT_HEALTHY;
            } else {
                return new HealthResult(
                    HealthStatus.UNHEALTHY,
                    String.format("Unexpected response code: %s returned from url: %s", response.code(), url));
            }
        } catch (final IOException e) {
            LOG.warn("Exception thrown trying to access url: {} during healthcheck", url, e);
            return new HealthResult(
                HealthStatus.UNHEALTHY,
                String.format("Exception thrown trying access url: %s during healthcheck", url));
        }
    }

    @Override
    public void delete(final String reference)
        throws DataStoreException
    {
        LOG.debug("Received delete request for data with reference: {}", reference);
        numDeleteRequests.incrementAndGet();
        Objects.requireNonNull(reference);
        final Request request = new Request.Builder().url(String.join("/", url, reference)).delete().build();
        LOG.debug("Deleting data at url: {}", request.url());
        try (final Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                LOG.debug("Successfully deleted data with reference: {}", reference);
            } else {
                numErrors.incrementAndGet();
                throw new DataStoreException(String.format(
                    "Failed to delete data with reference: %s. Response details: %s.", reference, response));
            }
        } catch (final IOException e) {
            numErrors.incrementAndGet();
            throw new DataStoreException("Failed to delete data with reference: " + reference, e);
        }
    }

    @Override
    public InputStream retrieve(final String reference)
        throws DataStoreException
    {
        LOG.debug("Received retrieve request for data with reference: {}", reference);
        numRetrieveRequests.incrementAndGet();
        Objects.requireNonNull(reference);
        final Request request = new Request.Builder().url(String.join("/", url, reference)).build();
        LOG.debug("Retrieving data at url: {}", request.url());
        try (final Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                final InputStream responseBodyInputStream = response.body().byteStream(); // TODO
                LOG.debug("Successfully retrieved data with reference: {}", reference);
                return responseBodyInputStream;
            } else {
                numErrors.incrementAndGet();
                throw new DataStoreException(String.format(
                    "Failed to retrieve data with reference: %s. Response details: %s.", reference, response));
            }
        } catch (final IOException e) {
            numErrors.incrementAndGet();
            throw new DataStoreException("Failed to retrieve data with reference: " + reference, e);
        }
    }

    @Override
    public long size(final String reference)
        throws DataStoreException
    {
        LOG.debug("Received size request for data with reference: {}", reference);
        Objects.requireNonNull(reference);
        final Request request = new Request.Builder().url(String.join("/", url, reference)).build();
        LOG.debug("Getting size of data at url: {}", request.url());
        try (final Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                final long responseBodyContentLength = response.body().contentLength(); // TODO
                LOG.debug("Successfully got size of data with reference: {}", reference);
                return responseBodyContentLength;
            } else {
                numErrors.incrementAndGet();
                throw new DataStoreException(String.format(
                    "Failed to get size of data with reference: %s. Response details: %s.", reference, response));
            }
        } catch (final IOException e) {
            numErrors.incrementAndGet();
            throw new DataStoreException("Failed to get size of data with reference: " + reference, e);
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
        final RequestBody requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", reference, RequestBody.create(data, OCTET_STREAM_MEDIA_TYPE))
            .build();
        final Request request = new Request.Builder().url(String.join("/", url, reference)).post(requestBody).build();
        LOG.debug("Storing data at url: {}", request.url());
        try (final Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                LOG.debug("Successfully stored data with reference: {}", reference);
                return reference;
            } else {
                numErrors.incrementAndGet();
                throw new DataStoreException(String.format(
                    "Failed to store data with reference: %s. Response details: %s.", reference, response));
            }
        } catch (final IOException e) {
            numErrors.incrementAndGet();
            throw new DataStoreException("Failed to store data with reference: " + reference, e);
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

    // TODO needed?
//    @Override
//    public OutputStream store(final String partialReference, final Consumer<String> setReferenceFunction)
//        throws DataStoreException
//    {
//        Objects.requireNonNull(setReferenceFunction);
//
//        try {
//            final Path ref = getStoreReference(partialReference);
//            final String reference = dataStorePath.relativize(ref).toString().replace('\\', '/');
//            final OutputStream fos = Files.newOutputStream(ref);
//            final BufferedOutputStream bos = new BufferedOutputStream(fos, outputBufferSize);
//            return new ProxyOutputStream(bos)
//            {
//                @Override
//                public void close() throws IOException
//                {
//                    super.close();
//                    setReferenceFunction.accept(reference);
//                }
//            };
//        } catch (final IOException e) {
//            numErrors.incrementAndGet();
//            throw new DataStoreException("Failed to get output stream for store", e);
//        }
//    }
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

//    private static int getOutputBufferSize(final HttpDataStoreConfiguration config)
//    {
//        final int DEFAULT_OUTPUT_BUFFER_SIZE = 16384;
//
//        final Integer configSetting = config.getOutputBufferSize();
//
//        return (configSetting == null || configSetting <= 0)
//            ? DEFAULT_OUTPUT_BUFFER_SIZE
//            : configSetting;
//    }
//
//    private static Duration getHealthcheckTimeoutSeconds(final HttpDataStoreConfiguration config)
//    {
//        final Integer configValue = config.getHealthcheckTimeoutSeconds();
//        return (configValue == null || configValue <= 0)
//            ? Duration.ofSeconds(DEFAULT_HEALTHCHECK_TIMEOUT_SECONDS)
//            : Duration.ofSeconds(configValue);
//    }
}
