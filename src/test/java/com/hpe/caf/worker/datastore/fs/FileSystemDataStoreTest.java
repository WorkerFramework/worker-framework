package com.hpe.caf.worker.datastore.fs;


import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


public class FileSystemDataStoreTest
{
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
    private File temp;
    private final String testData = "test123";
    private final String reference = "myRef.dat";


    @Before
    public void setUp()
        throws IOException
    {
        temp = tempDir.newFolder();
    }


    @Test
    public void testDataStore()
        throws ConfigurationException, DataStoreException, IOException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(temp.getAbsolutePath());
        DataStore store = new FileSystemDataStore(conf);
        final byte[] data = testData.getBytes(StandardCharsets.UTF_8);
        OutputStream outStr = store.getOutputStream(reference);
        outStr.write(data);
        outStr.close();
        InputStream inStr = store.getInputStream(reference);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int nRead;
        while ((nRead = inStr.read(data, 0, data.length)) != -1) {
            bos.write(data, 0, nRead);
        }
        bos.flush();
        Assert.assertArrayEquals(data, bos.toByteArray());
        Assert.assertEquals(testData.length(), store.getDataSize(reference));
    }
}
