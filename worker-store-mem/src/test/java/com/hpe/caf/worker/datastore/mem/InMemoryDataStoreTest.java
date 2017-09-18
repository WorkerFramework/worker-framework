/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.worker.datastore.mem;

import com.hpe.caf.api.worker.DataStoreException;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class InMemoryDataStoreTest
{
    private InMemoryDataStore dataStore;
    private String testStr;
    private String partialReference;

    @BeforeMethod
    private void setUp()
    {
        dataStore = new InMemoryDataStore();
        testStr = "This is a test string.";
        partialReference = UUID.randomUUID().toString();
    }

    @Test
    public void testDataStore() throws DataStoreException, IOException
    {
        byte[] input = testStr.getBytes();
        String absoluteReference = dataStore.store(input, partialReference);
        String retrievedString = IOUtils.toString(dataStore.retrieve(absoluteReference));
        Assert.assertEquals("Test string should equal the retrieved string.", testStr, retrievedString);
    }

    @Test
    public void testDataStore_inputStream() throws DataStoreException, IOException
    {
        InputStream input = new ByteArrayInputStream(testStr.getBytes(StandardCharsets.UTF_8));
        String absoluteReference = dataStore.store(input, partialReference);
        String retrievedString = IOUtils.toString(dataStore.retrieve(absoluteReference));
        Assert.assertEquals("Test string should equal the retrieved string.", testStr, retrievedString);
    }

    @Test
    public void testDataStore_path() throws DataStoreException, IOException
    {
        String fileLocation = "src/test/resources/testDataStore_path.txt";
        Path path = Paths.get(fileLocation);
        String absoluteReference = dataStore.store(path, partialReference);
        String retrievedString = IOUtils.toString(dataStore.retrieve(absoluteReference));
        String actualString = new String(Files.readAllBytes(path));
        Assert.assertEquals("Test string should equal the retrieved string.", actualString, retrievedString);
    }

    @Test(expectedExceptions = DataStoreException.class)
    public void testDelete() throws DataStoreException
    {
        byte[] input = testStr.getBytes();
        String absoluteReference = dataStore.store(input, partialReference);
        dataStore.delete(absoluteReference);
        // If the delete passes, then a retrieve will throw an exception as the reference does not exist.
        dataStore.retrieve(absoluteReference);
    }

    @Test
    public void testSizeMethod() throws DataStoreException, IOException
    {
        byte[] input = testStr.getBytes();
        String absoluteReference = dataStore.store(input, partialReference);
        long size = dataStore.size(absoluteReference);
        Assert.assertEquals("Test string bytes length should equal the retrieved size.", testStr.getBytes().length, size);
    }

    @Test(expectedExceptions = DataStoreException.class)
    public void testSizeMethod_failure() throws DataStoreException, IOException
    {
        byte[] input = testStr.getBytes();
        String absoluteReference = dataStore.store(input, partialReference);
        dataStore.delete(absoluteReference);
        long size = dataStore.size(absoluteReference);
    }
}
