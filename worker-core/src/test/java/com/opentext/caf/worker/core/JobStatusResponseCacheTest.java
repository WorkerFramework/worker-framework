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

import com.google.common.io.CharStreams;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JobStatusResponseCacheTest
{
    private static final String requestMethod = "GET";
    private static final String dummyResponseBody = "AbCdEfG";
    private static final int responseExpirySecs = 2;
    private static String maxAgeHeaderValue;
    private static URI dummyUri;

    private URLConnection mockConnection;
    private Map<String, List<String>> mockRequestHeaders;
    private Map<String, List<String>> dummyResponseHeaders;

    @BeforeClass
    public static void globalSetup() throws Exception
    {
        maxAgeHeaderValue = "max-age=" + String.valueOf(responseExpirySecs);
        dummyUri = new URI("http://thehost:1234/job-service/v1/jobs/1356184177/status");
    }

    @BeforeMethod
    public void perTestSetup() throws Exception
    {
        mockRequestHeaders = new HashMap<>();

        dummyResponseHeaders = new HashMap<>();
        dummyResponseHeaders.put("Transfer-Encoding", new ArrayList<>(Arrays.asList("chunked")));
        dummyResponseHeaders.put("Cache-Control", new ArrayList<>(Arrays.asList(maxAgeHeaderValue)));
        dummyResponseHeaders.put("Content-Type", new ArrayList<>(Arrays.asList("text/html")));

        mockConnection = Mockito.mock(URLConnection.class);
        Mockito.when(mockConnection.getHeaderFields()).thenReturn(dummyResponseHeaders);
        Mockito.when(mockConnection.getHeaderField("cache-control")).thenReturn(maxAgeHeaderValue);
    }

    @Test
    public void testPutGetCaching() throws Exception
    {
        // Include the header that indicates that responses can be cached by the Job Status cache.
        dummyResponseHeaders.put("CacheableJobStatus", new ArrayList<>(Arrays.asList("true")));
        Mockito.when(mockConnection.getHeaderField("CacheableJobStatus")).thenReturn("true");

        // The ResponseCache to be tested.
        JobStatusResponseCache cacheUnderTest = new JobStatusResponseCache();

        // Verify that nothing is cached for the URI to start with.
        Assert.assertNull(cacheUnderTest.get(dummyUri, requestMethod, mockRequestHeaders));

        // Cache a request.
        CacheRequest cacheRequest = cacheUnderTest.put(dummyUri, mockConnection);
        cacheRequest.getBody().write(dummyResponseBody.getBytes(StandardCharsets.UTF_8));

        // Retrieve the cached response and verify its contents. Do this twice to ensure it's repeatable.
        for (int i = 0; i <= 1; i++) {
            verifyCachedResponse(requestMethod, mockRequestHeaders, dummyUri, dummyResponseBody, dummyResponseHeaders, cacheUnderTest);
        }

        // Invalidate the cached request.
        cacheRequest.abort();

        // Verify that nothing is cached for the URI.
        Assert.assertNull(cacheUnderTest.get(dummyUri, requestMethod, mockRequestHeaders));

        // Re-cache and verify.
        cacheRequest = cacheUnderTest.put(dummyUri, mockConnection);
        cacheRequest.getBody().write(dummyResponseBody.getBytes(StandardCharsets.UTF_8));
        verifyCachedResponse(requestMethod, mockRequestHeaders, dummyUri, dummyResponseBody, dummyResponseHeaders, cacheUnderTest);

        // Allow the cached response to expire.
        Thread.sleep(responseExpirySecs * 1000 + 100);

        // Verify that nothing is cached for the URI.
        Assert.assertNull(cacheUnderTest.get(dummyUri, requestMethod, mockRequestHeaders));
    }

    @Test
    public void testNonCaching() throws Exception
    {
        // Deliberately omitting CacheableJobStatus header so the response should not be cached.

        // The ResponseCache to be tested.
        JobStatusResponseCache cacheUnderTest = new JobStatusResponseCache();

        // Verify that nothing is cached for the URI to start with.
        Assert.assertNull(cacheUnderTest.get(dummyUri, requestMethod, mockRequestHeaders));

        // Make a request - the response should not be cached as it does not supply the CacheableJobStatus header.
        CacheRequest cacheRequest = cacheUnderTest.put(dummyUri, mockConnection);

        // Verify that nothing has been cached for the URI.
        Assert.assertNull(cacheRequest);
        Assert.assertNull(cacheUnderTest.get(dummyUri, requestMethod, mockRequestHeaders));
    }

    private void verifyCachedResponse(String requestMethod, Map<String, List<String>> mockRequestHeaders, URI dummyUri, String dummyResponseBody, Map<String, List<String>> dummyResponseHeaders, JobStatusResponseCache cacheUnderTest)
        throws IOException
    {
        CacheResponse cachedResponse = cacheUnderTest.get(dummyUri, requestMethod, mockRequestHeaders);
        Assert.assertNotNull(cachedResponse);
        String retrievedResponseBody = CharStreams.toString(new InputStreamReader(cachedResponse.getBody(), StandardCharsets.UTF_8));
        Assert.assertEquals(dummyResponseBody, retrievedResponseBody);
        Map<String, List<String>> retrievedResponseHeaders = cachedResponse.getHeaders();
        Assert.assertEquals(dummyResponseHeaders.toString(), retrievedResponseHeaders.toString());
    }
}
