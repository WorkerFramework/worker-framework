/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An implementation of ResponseCache supporting the caching of job status responses.
 */
public class JobStatusResponseCache extends ResponseCache
{
    private static final String CACHEABLE_JOB_STATUS_HEADER_NAME = "CacheableJobStatus";
    private static final String CACHE_CONTROL_HEADER_NAME = "cache-control";
    private static final String CACHE_CONTROL_HEADER_MAX_AGE_PATTERN = "max-age=([0-9]*)?";
    private static final long DEFAULT_JOB_STATUS_CHECK_INTERVAL_MILLIS = 120000;
    private static final Logger LOG = LoggerFactory.getLogger(JobStatusResponseCache.class);

    private ResponseStreamCache jobStatusCache;

    public JobStatusResponseCache()
    {
        jobStatusCache = new ResponseStreamCache();
    }

    @Override
    public CacheResponse get(URI uri, String requestMethod, Map<String, List<String>> requestHeaders) throws IOException
    {
        ByteArrayOutputStream cachedResponseStream = jobStatusCache.get(uri);
        if (cachedResponseStream != null) {
            LOG.debug("Job status response cache hit for URI={}", uri);
            return new JobStatusCacheResponse(cachedResponseStream);
        }
        LOG.debug("Job status response cache miss for URI={}", uri);
        return null;
    }

    @Override
    public CacheRequest put(URI uri, URLConnection connection) throws IOException
    {
        if (!isCacheableJobStatusResponse(connection)) {
            LOG.debug("Uncacheable response from URI={}", uri);
            return null;
        }
        LOG.debug("Creating cached job status request for URI={}", uri);
        return new JobStatusCacheRequest(jobStatusCache, uri, connection);
    }

    private boolean isCacheableJobStatusResponse(URLConnection connection)
    {
        String cacheableJobStatus = connection.getHeaderField(CACHEABLE_JOB_STATUS_HEADER_NAME);
        if (cacheableJobStatus != null) {
            return Boolean.parseBoolean(cacheableJobStatus);
        }
        return false;
    }

    /**
     * Extracts the job status check interval from the relevant response header on the connection.
     *
     * @param connection extract the interval value from the headers on this connection
     * @return job status check interval in milliseconds
     */
    public static long getStatusCheckIntervalMillis(URLConnection connection)
    {
        long intervalMillis = getDefaultJobStatusCheckIntervalMillis();
        String cacheControl = connection.getHeaderField(CACHE_CONTROL_HEADER_NAME);
        if (cacheControl != null) {
            LOG.debug("{} = {}", CACHE_CONTROL_HEADER_NAME, cacheControl);
            Pattern maxAgeRegEx = Pattern.compile(CACHE_CONTROL_HEADER_MAX_AGE_PATTERN);
            Matcher maxAgeValueMatcher = maxAgeRegEx.matcher(cacheControl);
            if (maxAgeValueMatcher.find()) {
                intervalMillis = 1000 * Integer.parseInt(maxAgeValueMatcher.group(1));
                LOG.debug("Returning interval derived from {} header as {}ms", CACHE_CONTROL_HEADER_NAME, intervalMillis);
            }
        }
        return intervalMillis;
    }

    public static long getDefaultJobStatusCheckIntervalMillis()
    {
        return DEFAULT_JOB_STATUS_CHECK_INTERVAL_MILLIS;
    }
}
