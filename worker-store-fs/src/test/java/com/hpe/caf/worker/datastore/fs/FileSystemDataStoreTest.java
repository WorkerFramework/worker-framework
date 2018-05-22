/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.worker.datastore.fs;

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.FilePathProvider;
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

public class FileSystemDataStoreTest
{
    private File temp;
    private final String testData = "test123";

    @BeforeMethod
    public void setUp()
    {
        temp = new File("temp");
    }

    @AfterMethod
    public void tearDown()
    {
        deleteDir(temp);
    }

    private void deleteDir(File file)
    {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    @Test
    public void testDataStoreStream()
        throws ConfigurationException, DataStoreException, IOException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(temp.getAbsolutePath());
        DataStore store = new FileSystemDataStore(conf);
        final byte[] data = testData.getBytes(StandardCharsets.UTF_8);
        String storeRef = store.store(new ByteArrayInputStream(data), "test");
        verifyStoredData(store, data, storeRef);
        Assert.assertEquals(testData.length(), store.size(storeRef));
    }

    @Test
    public void testDataStoreStreamHash()
        throws ConfigurationException, DataStoreException, IOException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(temp.getAbsolutePath());
        DataStore store = new FileSystemDataStore(conf);
        final byte[] data = testData.getBytes(StandardCharsets.UTF_8);
        final HashStoreResult storeResult = StoreUtil.hashStore(store, new ByteArrayInputStream(data), "test");
        verifyStoredData(store, data, storeResult.getReference());
        Assert.assertEquals(testData.length(), store.size(storeResult.getReference()));
        Assert.assertEquals(DigestUtils.sha1Hex(data), storeResult.getHash());
    }

    @Test
    public void testDataStoreBytes()
        throws ConfigurationException, DataStoreException, IOException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(temp.getAbsolutePath());
        DataStore store = new FileSystemDataStore(conf);
        final byte[] data = testData.getBytes(StandardCharsets.UTF_8);
        String storeRef = store.store(data, "test");
        verifyStoredData(store, data, storeRef);
        Assert.assertEquals(testData.length(), store.size(storeRef));
    }

    @Test
    public void testDataStoreBytesHash()
        throws ConfigurationException, DataStoreException, IOException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(temp.getAbsolutePath());
        DataStore store = new FileSystemDataStore(conf);
        final byte[] data = testData.getBytes(StandardCharsets.UTF_8);
        final HashStoreResult storeResult = StoreUtil.hashStore(store, data, "test");
        verifyStoredData(store, data, storeResult.getReference());
        Assert.assertEquals(testData.length(), store.size(storeResult.getReference()));
        Assert.assertEquals(DigestUtils.sha1Hex(data), storeResult.getHash());
    }

    @Test
    public void testDataStorePath()
        throws ConfigurationException, DataStoreException, IOException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(temp.getAbsolutePath());
        DataStore store = new FileSystemDataStore(conf);
        final byte[] data = testData.getBytes(StandardCharsets.UTF_8);
        Path p = Paths.get(temp.getAbsolutePath()).resolve(UUID.randomUUID().toString());
        Files.write(p, data);
        String storeRef = store.store(p, "test");
        verifyStoredData(store, data, storeRef);
        Assert.assertEquals(testData.length(), store.size(storeRef));
    }

    @Test
    public void testDataStorePathHash()
        throws ConfigurationException, DataStoreException, IOException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(temp.getAbsolutePath());
        DataStore store = new FileSystemDataStore(conf);
        final byte[] data = testData.getBytes(StandardCharsets.UTF_8);
        Path p = Paths.get(temp.getAbsolutePath()).resolve(UUID.randomUUID().toString());
        Files.write(p, data);
        final HashStoreResult storeResult = StoreUtil.hashStore(store, p, "test");
        try (InputStream inStr = store.retrieve(storeResult.getReference())) {
            verifyData(data, inStr);
        }
        Assert.assertEquals(testData.length(), store.size(storeResult.getReference()));
        Assert.assertEquals(DigestUtils.sha1Hex(data), storeResult.getHash());
    }

    @Test
    public void testDataStoreFilePathRetrieval()
        throws ConfigurationException, DataStoreException, IOException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(temp.getAbsolutePath());
        DataStore store = new FileSystemDataStore(conf);
        final byte[] data = testData.getBytes(StandardCharsets.UTF_8);
        Path p = Paths.get(temp.getAbsolutePath()).resolve(UUID.randomUUID().toString());
        Files.write(p, data);
        String storeRef = store.store(p, "test");
        final Path dataStoreFilePath = ((FilePathProvider) store).getFilePath(storeRef);
        try (InputStream inStr = Files.newInputStream(dataStoreFilePath)) {
            verifyData(data, inStr);
        }
        //Verify that repeatable values are returned for the file path of the same stored file.
        Assert.assertEquals(((FilePathProvider) store).getFilePath(storeRef), dataStoreFilePath);
    }

    @Test(expectedExceptions = DataStoreException.class)
    public void testInvalidReference()
        throws DataStoreException, IOException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(temp.getAbsolutePath());
        DataStore store = new FileSystemDataStore(conf);
        Path p = Paths.get(temp.getAbsolutePath());
        for (int i = 0; i < 5; i++) {
            p = p.resolve("..");
        }
        store.retrieve(p.toString());
    }

    @Test(expectedExceptions = DataStoreException.class)
    public void testInvalidReferenceFilePathRetrieval()
        throws DataStoreException, IOException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(temp.getAbsolutePath());
        DataStore store = new FileSystemDataStore(conf);
        Path p = Paths.get(temp.getAbsolutePath());
        for (int i = 0; i < 5; i++) {
            p = p.resolve("..");
        }
        ((FilePathProvider) store).getFilePath(p.toString());
    }

    @Test(expectedExceptions = ReferenceNotFoundException.class)
    public void testMissingRef()
        throws DataStoreException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(temp.getAbsolutePath());
        DataStore store = new FileSystemDataStore(conf);
        store.retrieve(UUID.randomUUID().toString());
    }

    @Test(expectedExceptions = ReferenceNotFoundException.class)
    public void testMissingRefFilePathRetrieval()
        throws DataStoreException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(temp.getAbsolutePath());
        DataStore store = new FileSystemDataStore(conf);
        ((FilePathProvider) store).getFilePath(UUID.randomUUID().toString());
    }

    @Test
    public void testDeleteWithValidReference()
        throws DataStoreException, IOException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(temp.getAbsolutePath());
        DataStore store = new FileSystemDataStore(conf);
        final byte[] data = testData.getBytes(StandardCharsets.UTF_8);
        String storeRef = store.store(data, "test");

        Path p = Paths.get(temp.toString(), storeRef);

        Assert.assertTrue(Files.exists(p));
        store.delete(storeRef);
        Assert.assertFalse(Files.exists(p));
    }

    @Test(expectedExceptions = DataStoreException.class)
    public void testDeleteWithInvalidReference()
        throws DataStoreException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(temp.getAbsolutePath());
        DataStore store = new FileSystemDataStore(conf);
        store.delete(UUID.randomUUID().toString());
    }

    private static void verifyStoredData(final DataStore dataStore, final byte[] expectedData, final String actualReference)
        throws IOException, DataStoreException
    {
        try (InputStream inStr = dataStore.retrieve(actualReference)) {
            verifyData(expectedData, inStr);
        }
    }

    private static void verifyData(final byte[] expected, final InputStream actual)
        throws IOException
    {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            int nRead;
            final byte[] buffer = new byte[1024];
            while ((nRead = actual.read(buffer, 0, expected.length)) != -1) {
                bos.write(buffer, 0, nRead);
            }
            bos.flush();
            ArrayAsserts.assertArrayEquals(expected, bos.toByteArray());
        }
    }
}
