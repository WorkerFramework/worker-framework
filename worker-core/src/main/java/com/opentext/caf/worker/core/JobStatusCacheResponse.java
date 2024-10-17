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
package com.opentext.caf.worker.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.CacheResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An implementation of CacheResponse supporting the caching of job status responses.
 */
public class JobStatusCacheResponse extends CacheResponse
{
    private static final Logger LOG = LoggerFactory.getLogger(JobStatusCacheResponse.class);

    private ByteArrayInputStream responseBody;
    private Map<String, List<String>> responseHeaders;

    public JobStatusCacheResponse(ByteArrayOutputStream cachedResponseStream) throws IOException
    {
        Objects.requireNonNull(cachedResponseStream);
        responseBody = new ByteArrayInputStream(cachedResponseStream.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(responseBody);
        try {
            responseHeaders = (Map<String, List<String>>) ois.readObject();
        } catch (ClassNotFoundException e) {
            LOG.error("Failed to read cached job status response headers. ", e);
            responseHeaders = Collections.emptyMap();
        }
    }

    @Override
    public Map<String, List<String>> getHeaders() throws IOException
    {
        return responseHeaders;
    }

    @Override
    public InputStream getBody() throws IOException
    {
        return responseBody;
    }
}
