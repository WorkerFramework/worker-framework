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
package com.hpe.caf.worker.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

/**
 * Provides in-memory caching of web responses on per-URI basis.
 */
public class ResponseStreamCache
{
    private static final long CACHE_MAX_SIZE = 1000;
    private static final long CACHE_ITEM_LIFETIME_SECS = 3600; //Store each cached response for up to 1 hour.

    // Underlying in-memory cache.
    private final Cache<URI,Entry> cacheImpl;

    public ResponseStreamCache()
    {
        this.cacheImpl = CacheBuilder.newBuilder()
            .maximumSize(CACHE_MAX_SIZE)
            .expireAfterWrite(CACHE_ITEM_LIFETIME_SECS, TimeUnit.SECONDS)
            .build();
    }

    public void put(final URI uri, ByteArrayOutputStream baos, long lifetimeMillis, URLConnection urlConnection) throws IOException
    {
        if (urlConnection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection)urlConnection;
            Certificate[] localCertificates = httpsURLConnection.getLocalCertificates();
            Certificate[] serverCertificates = httpsURLConnection.getServerCertificates();
            cacheImpl.put(
                    uri,
                    new SecureResponseStreamCacheEntry(
                            System.currentTimeMillis() + lifetimeMillis,
                            baos,
                            httpsURLConnection.getCipherSuite(),
                            localCertificates == null ? null : Arrays.asList(localCertificates),
                            serverCertificates == null ? null : Arrays.asList(serverCertificates),
                            httpsURLConnection.getPeerPrincipal(),
                            httpsURLConnection.getLocalPrincipal()));
        } else {
            cacheImpl.put(
                    uri,
                    new ResponseStreamCacheEntry(
                            System.currentTimeMillis() + lifetimeMillis,
                            baos));
        }
    }

    public Entry get(final URI uri)
    {
        checkExpiry(uri);
        return cacheImpl.getIfPresent(uri);
    }

    public void remove(final URI uri)
    {
        cacheImpl.invalidate(uri);
    }

    private void checkExpiry(final URI uri)
    {
        Entry cacheEntry = cacheImpl.getIfPresent(uri);
        if (cacheEntry != null && System.currentTimeMillis() >= cacheEntry.getExpiryTimeMillis()) {
            cacheImpl.invalidate(uri);
        }
    }

    public static abstract class Entry
    {
        private long expiryTimeMillis;
        private ByteArrayOutputStream responseStream;

        public Entry(long expiryTimeMillis, ByteArrayOutputStream responseStream)
        {
            this.expiryTimeMillis = expiryTimeMillis;
            this.responseStream = responseStream;
        }

        public long getExpiryTimeMillis()
        {
            return expiryTimeMillis;
        }

        public ByteArrayOutputStream getResponseStream()
        {
            return responseStream;
        }
    }

    public static class ResponseStreamCacheEntry extends Entry
    {
        public ResponseStreamCacheEntry(final long expiryTimeMillis, final ByteArrayOutputStream responseStream)
        {
            super(expiryTimeMillis, responseStream);
        }
    }

    public static class SecureResponseStreamCacheEntry extends Entry
    {
        private String cipherSuite;
        private List<Certificate> localCertificateChain;
        private List<Certificate> serverCertificateChain;
        private Principal peerPrincipal;
        private Principal localPrincipal;

        public SecureResponseStreamCacheEntry(
                long expiryTimeMillis,
                ByteArrayOutputStream responseStream,
                String cipherSuite,
                List<Certificate> localCertificateChain,
                List<Certificate> serverCertificateChain,
                Principal peerPrincipal,
                Principal localPrincipal)
        {
            super(expiryTimeMillis, responseStream);
            this.cipherSuite = cipherSuite;
            this.localCertificateChain = localCertificateChain;
            this.serverCertificateChain = serverCertificateChain;
            this.peerPrincipal = peerPrincipal;
            this.localPrincipal = localPrincipal;
        }

        public String getCipherSuite()
        {
            return cipherSuite;
        }

        public List<Certificate> getLocalCertificateChain()
        {
            return localCertificateChain;
        }

        public List<Certificate> getServerCertificateChain()
        {
            return serverCertificateChain;
        }

        public Principal getPeerPrincipal()
        {
            return peerPrincipal;
        }

        public Principal getLocalPrincipal()
        {
            return localPrincipal;
        }
    }
}
