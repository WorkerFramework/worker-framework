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
package com.github.workerframework.worker.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.SecureCacheResponse;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of SecureCacheResponse supporting the caching of job status responses.
 */
public class JobStatusSecureCacheResponse extends SecureCacheResponse
{
    private static final Logger LOG = LoggerFactory.getLogger(JobStatusSecureCacheResponse.class);

    private ByteArrayInputStream responseBody;
    private Map<String, List<String>> responseHeaders;
    private String cipherSuite;
    private List<Certificate> localCertificateChain;
    private List<Certificate> serverCertificateChain;
    private Principal peerPrincipal;
    private Principal localPrincipal;

    public JobStatusSecureCacheResponse(
            ByteArrayOutputStream cachedResponseStream,
            String cipherSuite,
            List<Certificate> localCertificateChain,
            List<Certificate> serverCertificateChain,
            Principal peerPrincipal,
            Principal localPrincipal)
            throws IOException
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
        this.cipherSuite = cipherSuite;
        this.localCertificateChain = localCertificateChain;
        this.serverCertificateChain = serverCertificateChain;
        this.peerPrincipal = peerPrincipal;
        this.localPrincipal = localPrincipal;
    }

    @Override
    public Map<String, List<String>> getHeaders()
    {
        return responseHeaders;
    }

    @Override
    public InputStream getBody()
    {
        return responseBody;
    }

    @Override
    public String getCipherSuite()
    {
        return cipherSuite;
    }

    @Override
    public List<Certificate> getLocalCertificateChain()
    {
        return localCertificateChain;
    }

    @Override
    public List<Certificate> getServerCertificateChain()
    {
        return serverCertificateChain;
    }

    @Override
    public Principal getPeerPrincipal()
    {
        return peerPrincipal;
    }

    @Override
    public Principal getLocalPrincipal()
    {
        return localPrincipal;
    }
}
