package com.hpe.caf.worker.datastore.inmemory;

import com.hpe.caf.api.worker.DataStoreException;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class InMemoryDataStoreTest
{
    InMemoryDataStore dataStore;

    @BeforeMethod
    private void setUp()
    {
        dataStore = new InMemoryDataStore();
    }

    @Test
    public void testDataStore() throws DataStoreException, IOException
    {
        String testStr = "This is a test string.";
        String partialReference = "123456789123456789";
        byte[] input = testStr.getBytes();

        String absoluteReference = dataStore.store(input, partialReference);

        String retrievedString = IOUtils.toString(dataStore.retrieve(absoluteReference));
        Assert.assertEquals("Test string should equal the retrieved string.", testStr, retrievedString);
    }

    @Test
    public void testDataStore_inputStream() throws DataStoreException, IOException
    {
        String testStr = "This is a test string.";
        String partialReference = "123456789123456789";
        InputStream input = new ByteArrayInputStream(testStr.getBytes(StandardCharsets.UTF_8));

        String absoluteReference = dataStore.store(input, partialReference);

        String retrievedString = IOUtils.toString(dataStore.retrieve(absoluteReference));
        Assert.assertEquals("Test string should equal the retrieved string.", testStr, retrievedString);
    }

    @Test
    public void testDataStore_path() throws DataStoreException, IOException
    {
        String testStr = "This is a test string.";
        String partialReference = "123456789123456789";
//        InputStream input = new ByteArrayInputStream(testStr.getBytes(StandardCharsets.UTF_8));
//
//        String absoluteReference = dataStore.store(input, partialReference);
//
//        String retrievedString = IOUtils.toString(dataStore.retrieve(absoluteReference));
//        Assert.assertEquals("Test string should equal the retrieved string.", testStr, retrievedString);
    }

    @Test(expectedExceptions = DataStoreException.class)
    public void testDelete() throws DataStoreException
    {
        String testStr = "This is a test string.";
        String partialReference = "123456789123456789";
        byte[] input = testStr.getBytes();

        String absoluteReference = dataStore.store(input, partialReference);

        dataStore.delete(absoluteReference);

        dataStore.retrieve(absoluteReference);
    }


}
