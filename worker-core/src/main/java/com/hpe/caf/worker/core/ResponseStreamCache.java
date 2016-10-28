package com.hpe.caf.worker.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Provides in-memory caching of web responses on per-URI basis.
 */
public class ResponseStreamCache {
    private static final long CACHE_MAX_SIZE = 1000;
    private static final long CACHE_ITEM_LIFETIME_SECS = 3600; //Store each cached response for up to 1 hour.

    // Underlying in-memory cache.
    private final Cache<URI, ResponseStreamCacheEntry> cacheImpl;


    public ResponseStreamCache() {
        this.cacheImpl = CacheBuilder.newBuilder()
                .maximumSize(CACHE_MAX_SIZE)
                .expireAfterWrite(CACHE_ITEM_LIFETIME_SECS, TimeUnit.SECONDS)
                .build();
    }


    public void put(final URI uri, ByteArrayOutputStream baos, long lifetimeMillis) {
        cacheImpl.put(uri, new ResponseStreamCacheEntry(System.currentTimeMillis() + lifetimeMillis, baos));
    }


    public ByteArrayOutputStream get(final URI uri) {
        checkExpiry(uri);
        ResponseStreamCacheEntry cacheEntry = cacheImpl.getIfPresent(uri);
        return cacheEntry == null ? null : cacheEntry.getResponseStream();
    }


    public void remove(final URI uri) {
        cacheImpl.invalidate(uri);
    }


    private void checkExpiry(final URI uri) {
        ResponseStreamCacheEntry cacheEntry = cacheImpl.getIfPresent(uri);
        if (cacheEntry != null && System.currentTimeMillis() >= cacheEntry.getExpiryTimeMillis()) {
            cacheImpl.invalidate(uri);
        }
    }


    private static class ResponseStreamCacheEntry {
        private long expiryTimeMillis;
        private ByteArrayOutputStream responseStream;

        public ResponseStreamCacheEntry(long expiryTimeMillis, ByteArrayOutputStream baos) {
            this.expiryTimeMillis = expiryTimeMillis;
            this.responseStream = baos;
        }

        public long getExpiryTimeMillis() {
            return expiryTimeMillis;
        }

        public ByteArrayOutputStream getResponseStream() {
            return responseStream;
        }
    }
}
