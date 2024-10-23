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
import com.github.workerframework.worker.api.DataStore;
import com.github.workerframework.worker.api.DataStoreException;
import com.github.workerframework.worker.api.ReferenceNotFoundException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.internal.junit.ArrayAsserts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public class HttpDataStoreTest
{
    private static final String PARTIAL_REFERENCE = "partial-reference";
    private static final String TEST_DATA = "test123";
    private TestHttpServer testHttpServer;
    private HttpDataStoreConfiguration config;
    private File temporaryFolder;

    @BeforeClass
    public void beforeClassSetup() throws IOException
    {
        testHttpServer = new TestHttpServer();
        config = createConfig(testHttpServer.getPort());
    }

    @AfterClass
    public void afterClassTeardown() throws Exception
    {
        testHttpServer.close();
    }

    @BeforeMethod
    public void createTemporaryFolder()
    {
        temporaryFolder = new File("temp");
        if (!temporaryFolder.exists()) {
            temporaryFolder.mkdir();
        }
    }

    @AfterMethod
    public void deleteTemporaryFolder()
    {
        deleteDir(temporaryFolder);
    }

    private void deleteDir(final File file)
    {
        final File[] contents = file.listFiles();
        if (contents != null) {
            for (final File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    @Test
    public void testStoreInputStream() throws Exception
    {
        final DataStore httpDataStore = new HttpDataStore(config);
        final byte[] testDataBytes = TEST_DATA.getBytes(StandardCharsets.UTF_8);
        final String storeReference = httpDataStore.store(new ByteArrayInputStream(testDataBytes), PARTIAL_REFERENCE);
        verifyStoredData(httpDataStore, TEST_DATA.getBytes(StandardCharsets.UTF_8), storeReference);
        Assert.assertEquals(httpDataStore.size(storeReference), TEST_DATA.length(), "Size of stored data not as expected");
    }

    @Test
    public void testStoreByteArray() throws Exception
    {
        final DataStore httpDataStore = new HttpDataStore(config);
        final byte[] testDataBytes = TEST_DATA.getBytes(StandardCharsets.UTF_8);
        final String storeReference = httpDataStore.store(testDataBytes, PARTIAL_REFERENCE);
        verifyStoredData(httpDataStore, testDataBytes, storeReference);
        Assert.assertEquals(httpDataStore.size(storeReference), TEST_DATA.length(), "Size of stored data not as expected");
    }

    @Test
    public void testStoreFile() throws Exception
    {
        final DataStore httpDataStore = new HttpDataStore(config);
        final byte[] testDataBytes = TEST_DATA.getBytes(StandardCharsets.UTF_8);
        final Path path = Paths.get(temporaryFolder.getAbsolutePath()).resolve(UUID.randomUUID().toString());
        Files.write(path, testDataBytes);
        final String storeReference = httpDataStore.store(path, "test");
        verifyStoredData(httpDataStore, testDataBytes, storeReference);
        Assert.assertEquals(httpDataStore.size(storeReference), TEST_DATA.length(), "Size of stored data not as expected");
    }

    @Test(expectedExceptions = {ReferenceNotFoundException.class})
    public void testDeleteData() throws DataStoreException, IOException
    {
        // Store
        final DataStore httpDataStore = new HttpDataStore(config);
        final byte[] testDataBytes = TEST_DATA.getBytes(StandardCharsets.UTF_8);
        final String storeReference = httpDataStore.store(new ByteArrayInputStream(testDataBytes), PARTIAL_REFERENCE);

        // Retrieve before deletion
        verifyStoredData(httpDataStore, TEST_DATA.getBytes(StandardCharsets.UTF_8), storeReference);
        Assert.assertEquals(httpDataStore.size(storeReference), TEST_DATA.length(), "Size of stored data not as expected");

        // Delete
        httpDataStore.delete(storeReference);

        // Retrieve after deletion
        httpDataStore.retrieve(storeReference);
    }

    @Test
    public void testHealthcheck() throws DataStoreException
    {
        final HttpDataStore httpDataStore = new HttpDataStore(config);
        Assert.assertEquals(httpDataStore.healthCheck(), HealthResult.RESULT_HEALTHY, "Healthcheck status should be HEALTHY");
    }

    @Test
    public void testHealthcheckUnhealthy() throws DataStoreException
    {
        final HttpDataStoreConfiguration configWithBadUrl = new HttpDataStoreConfiguration();
        configWithBadUrl.setUrl("http://idontexist:1234");
        final HttpDataStore httpDataStore = new HttpDataStore(configWithBadUrl);
        final HealthResult healthResult = httpDataStore.healthCheck();
        Assert.assertEquals(healthResult.getStatus(), HealthStatus.UNHEALTHY, "Healthcheck status should be UNHEALTHY");
        Assert.assertEquals(healthResult.getMessage(),
                            "Exception thrown trying access url: http://idontexist:1234 during healthcheck",
                            "Healthcheck message is incorrect");
    }

    private static HttpDataStoreConfiguration createConfig(final int httpServerPort)
    {
        final HttpDataStoreConfiguration config = new HttpDataStoreConfiguration();
        config.setUrl("http://localhost:" + httpServerPort);
        config.setConnectTimeoutMillis(30000);
        config.setReadTimeoutMillis(30000);
        return config;
    }

    private static void verifyStoredData(final DataStore dataStore, final byte[] expectedData, final String actualReference)
        throws IOException, DataStoreException
    {
        try (final InputStream inputStream = dataStore.retrieve(actualReference)) {
            verifyData(expectedData, inputStream);
        }
    }

    private static void verifyData(final byte[] expected, final InputStream actual)
        throws IOException
    {
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            int nRead;
            final byte[] buffer = new byte[1024];
            while ((nRead = actual.read(buffer, 0, expected.length)) != -1) {
                byteArrayOutputStream.write(buffer, 0, nRead);
            }
            byteArrayOutputStream.flush();
            ArrayAsserts.assertArrayEquals("Stored data not as expected", expected, byteArrayOutputStream.toByteArray());
        }
    }
}
