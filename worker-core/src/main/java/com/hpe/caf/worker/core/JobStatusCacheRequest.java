/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.CacheRequest;
import java.net.URI;
import java.net.URLConnection;
import java.util.Objects;

/**
 * An implementation of CacheRequest supporting the caching of job status requests.
 */
public class JobStatusCacheRequest extends CacheRequest
{
    private static final Logger LOG = LoggerFactory.getLogger(JobStatusCacheRequest.class);

    private ResponseStreamCache jobStatusCache;
    private URI uri;
    private ByteArrayOutputStream responseStream;

    public JobStatusCacheRequest(ResponseStreamCache jobStatusCache, final URI uri, URLConnection connection) throws IOException
    {
        Objects.requireNonNull(jobStatusCache);
        Objects.requireNonNull(connection);
        this.jobStatusCache = jobStatusCache;
        this.uri = uri;
        this.responseStream = new ByteArrayOutputStream();
        this.jobStatusCache.put(uri, responseStream, JobStatusResponseCache.getStatusCheckIntervalMillis(connection));
        LOG.debug("Writing job status request headers to cache for URI={}", uri);
        ObjectOutputStream oos = new ObjectOutputStream(this.responseStream);
        oos.writeObject(connection.getHeaderFields());
    }

    @Override
    public OutputStream getBody() throws IOException
    {
        LOG.debug("Returning cached job status request stream for URI={}", uri);
        return responseStream;
    }

    @Override
    public void abort()
    {
        try {
            LOG.debug("Removing cached job status for URI={}", uri);
            responseStream.close();
            jobStatusCache.remove(uri);
        } catch (IOException e) {
            LOG.error("Error occurred when removing cached job status for URI={}", uri, e);
        }
    }
}
