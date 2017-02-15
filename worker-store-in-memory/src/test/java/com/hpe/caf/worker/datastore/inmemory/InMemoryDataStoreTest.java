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
        dataStore.retrieve(absoluteReference);
    }

}
