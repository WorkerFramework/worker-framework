package com.hpe.caf.worker.core;

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
public class JobStatusCacheResponse extends CacheResponse {
    private static final Logger LOG = LoggerFactory.getLogger(JobStatusCacheResponse.class);

    private ByteArrayInputStream responseBody;
    Map<String, List<String>> responseHeaders;


    public JobStatusCacheResponse(ByteArrayOutputStream cachedResponseStream) throws IOException {
        Objects.requireNonNull(cachedResponseStream);
        responseBody = new ByteArrayInputStream(cachedResponseStream.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(responseBody);
        try {
            responseHeaders = (Map<String, List<String>>) ois.readObject();
        } catch (ClassNotFoundException e) {
            LOG.error("Failed to read cached job status response headers. ", e);
            responseHeaders = Collections.EMPTY_MAP;
        }
    }


    @Override
    public Map getHeaders() throws IOException {
        return responseHeaders;
    }


    @Override
    public InputStream getBody() throws IOException {
        return responseBody;
    }
}
