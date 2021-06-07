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

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.ReferenceNotFoundException;
import com.hpe.caf.util.store.HashStoreResult;
import com.hpe.caf.util.store.StoreUtil;
import org.apache.commons.codec.digest.DigestUtils;
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
    private File temporaryFolder;

    @BeforeClass
    public void startHttpServer() throws IOException
    {
        testHttpServer = new TestHttpServer();
    }

    @AfterClass
    public void stopHttpServer() throws Exception
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
        final HttpDataStoreConfiguration config = createConfig(testHttpServer.getPort());
        final DataStore httpDataStore = new HttpDataStore(config);
        final byte[] testDataBytes = TEST_DATA.getBytes(StandardCharsets.UTF_8);
        final String storeReference = httpDataStore.store(new ByteArrayInputStream(testDataBytes), PARTIAL_REFERENCE);
        verifyStoredData(httpDataStore, TEST_DATA.getBytes(StandardCharsets.UTF_8), storeReference);
        Assert.assertEquals(httpDataStore.size(storeReference), TEST_DATA.length(), "Size of stored data not as expected");
    }

    @Test
    public void testStoreByteArray() throws Exception
    {
        final HttpDataStoreConfiguration config = createConfig(testHttpServer.getPort());
        final DataStore httpDataStore = new HttpDataStore(config);
        final byte[] testDataBytes = TEST_DATA.getBytes(StandardCharsets.UTF_8);
        final String storeReference = httpDataStore.store(testDataBytes, PARTIAL_REFERENCE);
        verifyStoredData(httpDataStore, testDataBytes, storeReference);
        Assert.assertEquals(httpDataStore.size(storeReference), TEST_DATA.length(), "Size of stored data not as expected");
    }

    @Test
    public void testStoreFile() throws Exception
    {
        final HttpDataStoreConfiguration config = createConfig(testHttpServer.getPort());
        final DataStore httpDataStore = new HttpDataStore(config);
        final byte[] testDataBytes = TEST_DATA.getBytes(StandardCharsets.UTF_8);
        final Path path = Paths.get(temporaryFolder.getAbsolutePath()).resolve(UUID.randomUUID().toString());
        Files.write(path, testDataBytes);
        final String storeReference = httpDataStore.store(path, "test");
        verifyStoredData(httpDataStore, testDataBytes, storeReference);
        Assert.assertEquals(httpDataStore.size(storeReference), TEST_DATA.length(), "Size of stored data not as expected");
    }

    @Test
    public void testStoreInputStreamWithHash()
        throws ConfigurationException, DataStoreException, IOException
    {
        final HttpDataStoreConfiguration config = createConfig(testHttpServer.getPort());
        final DataStore httpDataStore = new HttpDataStore(config);
        final byte[] testDataBytes = TEST_DATA.getBytes(StandardCharsets.UTF_8);
        final HashStoreResult storeResult
            = StoreUtil.hashStore(httpDataStore, new ByteArrayInputStream(testDataBytes), PARTIAL_REFERENCE);
        verifyStoredData(httpDataStore, testDataBytes, storeResult.getReference());
        Assert.assertEquals(TEST_DATA.length(), httpDataStore.size(storeResult.getReference()));
        Assert.assertEquals(DigestUtils.sha1Hex(testDataBytes), storeResult.getHash());
    }

    @Test
    public void testStoreBytesWithHash()
        throws ConfigurationException, DataStoreException, IOException
    {
        final HttpDataStoreConfiguration config = createConfig(testHttpServer.getPort());
        final DataStore httpDataStore = new HttpDataStore(config);
        final byte[] testDataBytes = TEST_DATA.getBytes(StandardCharsets.UTF_8);
        final HashStoreResult storeResult = StoreUtil.hashStore(httpDataStore, testDataBytes, PARTIAL_REFERENCE);
        verifyStoredData(httpDataStore, testDataBytes, storeResult.getReference());
        Assert.assertEquals(TEST_DATA.length(), httpDataStore.size(storeResult.getReference()));
        Assert.assertEquals(DigestUtils.sha1Hex(testDataBytes), storeResult.getHash());
    }

    @Test
    public void testStoreFileWithHash()
        throws ConfigurationException, DataStoreException, IOException
    {
        final HttpDataStoreConfiguration config = createConfig(testHttpServer.getPort());
        final DataStore httpDataStore = new HttpDataStore(config);
        final byte[] testDataBytes = TEST_DATA.getBytes(StandardCharsets.UTF_8);
        Path p = Paths.get(temporaryFolder.getAbsolutePath()).resolve(UUID.randomUUID().toString());
        Files.write(p, testDataBytes);
        final HashStoreResult storeResult = StoreUtil.hashStore(httpDataStore, p, "test");
        try (InputStream inStr = httpDataStore.retrieve(storeResult.getReference())) {
            verifyData(testDataBytes, inStr);
        }
        Assert.assertEquals(TEST_DATA.length(), httpDataStore.size(storeResult.getReference()));
        Assert.assertEquals(DigestUtils.sha1Hex(testDataBytes), storeResult.getHash());
    }
//
//    @Test
//    public void testDataStoreFilePathRetrieval()
//        throws ConfigurationException, DataStoreException, IOException
//    {
//        FileSystemDataStoreConfiguration conf = createConfig();
//        DataStore store = new FileSystemDataStore(conf);
//        final byte[] data = TEST_DATA.getBytes(StandardCharsets.UTF_8);
//        Path p = Paths.get(temporaryFolder.getAbsolutePath()).resolve(UUID.randomUUID().toString());
//        Files.write(p, data);
//        String storeRef = store.store(p, "test");
//        final Path dataStoreFilePath = ((FilePathProvider) store).getFilePath(storeRef);
//        try (InputStream inStr = Files.newInputStream(dataStoreFilePath)) {
//            verifyData(data, inStr);
//        }
//        //Verify that repeatable values are returned for the file path of the same stored file.
//        Assert.assertEquals(((FilePathProvider) store).getFilePath(storeRef), dataStoreFilePath);
//    }
//
//    @Test
//    public void testDataStoreOutputStream()
//        throws ConfigurationException, DataStoreException, IOException
//    {
//        final FileSystemDataStoreConfiguration conf = createConfig();
//        final FileSystemDataStore store = new FileSystemDataStore(conf);
//        final byte[] data = TEST_DATA.getBytes(StandardCharsets.UTF_8);
//        final ArrayList<String> storeRefs = new ArrayList<>();
//        try (final OutputStream outputStream = store.store("test", storeRefs::add)) {
//            outputStream.write(data);
//        }
//        Assert.assertEquals(storeRefs.size(), 1);
//        final String storeRef = storeRefs.get(0);
//        verifyStoredData(store, data, storeRef);
//        Assert.assertEquals(store.size(storeRef), data.length);
//    }
//
//    @Test(expectedExceptions = DataStoreException.class)
//    public void testInvalidReference()
//        throws DataStoreException, IOException
//    {
//        FileSystemDataStoreConfiguration conf = createConfig();
//        DataStore store = new FileSystemDataStore(conf);
//        Path p = Paths.get(temporaryFolder.getAbsolutePath());
//        for (int i = 0; i < 5; i++) {
//            p = p.resolve("..");
//        }
//        store.retrieve(p.toString());
//    }
//
//    @Test(expectedExceptions = DataStoreException.class)
//    public void testInvalidReferenceFilePathRetrieval()
//        throws DataStoreException, IOException
//    {
//        FileSystemDataStoreConfiguration conf = createConfig();
//        DataStore store = new FileSystemDataStore(conf);
//        Path p = Paths.get(temporaryFolder.getAbsolutePath());
//        for (int i = 0; i < 5; i++) {
//            p = p.resolve("..");
//        }
//        ((FilePathProvider) store).getFilePath(p.toString());
//    }
//
//    @Test(expectedExceptions = ReferenceNotFoundException.class)
//    public void testMissingRef()
//        throws DataStoreException
//    {
//        FileSystemDataStoreConfiguration conf = createConfig();
//        DataStore store = new FileSystemDataStore(conf);
//        store.retrieve(UUID.randomUUID().toString());
//    }
//
//    @Test(expectedExceptions = ReferenceNotFoundException.class)
//    public void testMissingRefFilePathRetrieval()
//        throws DataStoreException
//    {
//        FileSystemDataStoreConfiguration conf = createConfig();
//        DataStore store = new FileSystemDataStore(conf);
//        ((FilePathProvider) store).getFilePath(UUID.randomUUID().toString());
//    }
//

    @Test
    public void testDeleteData()
        throws DataStoreException, IOException
    {
        // Store
        final HttpDataStoreConfiguration config = createConfig(testHttpServer.getPort());
        final DataStore httpDataStore = new HttpDataStore(config);
        final byte[] testDataBytes = TEST_DATA.getBytes(StandardCharsets.UTF_8);
        final String storeReference = httpDataStore.store(new ByteArrayInputStream(testDataBytes), PARTIAL_REFERENCE);

        // Retrieve before deletion
        verifyStoredData(httpDataStore, TEST_DATA.getBytes(StandardCharsets.UTF_8), storeReference);
        Assert.assertEquals(httpDataStore.size(storeReference), TEST_DATA.length(), "Size of stored data not as expected");

        // Delete
        httpDataStore.delete(storeReference);

        // Retrieve after deletion
        try {
            httpDataStore.retrieve(storeReference);
            Assert.fail(
                "Expected a ReferenceNotFoundException exception to be thrown when trying to retrieve data that has been deleted");
        } catch (final ReferenceNotFoundException e) {
            // Expected
        }
    }

    @Test
    public void testHealthcheck()
        throws ConfigurationException, DataStoreException, IOException
    {
        final HttpDataStoreConfiguration config = createConfig(testHttpServer.getPort());
        final HttpDataStore httpDataStore = new HttpDataStore(config);
        Assert.assertEquals(httpDataStore.healthCheck(), HealthResult.RESULT_HEALTHY, "Healthcheck status should be HEALTHY");
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
