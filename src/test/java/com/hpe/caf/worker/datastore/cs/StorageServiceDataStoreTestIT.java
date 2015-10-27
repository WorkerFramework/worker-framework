package com.hpe.caf.worker.datastore.cs;

import com.hpe.caf.api.worker.DataStoreException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static junit.framework.Assert.assertEquals;

public class StorageServiceDataStoreTestIT {

    private static final String SERVER_NAME = "a1-dev-api052.lab.lynx-connected.com";
    private static final int SERVER_PORT = 8443;
    private static final String CONTAINER_ID = "5e94c95bf5aa426e8de876e80fd34bed";

    private static final String TEST_STRING = " ং ঃ অ আ ই ঈ উ ঊ ঋ ঌ এ ঐ ও ঔ ক খ গ ঘ ঙ চ ছ জ ঝ ঞ ট ঠ";

    StorageServiceDataStore storageServiceDataStore;

    @Before
    public void setUp(){
        StorageServiceDataStoreConfiguration storageServiceDataStoreConfiguration = new StorageServiceDataStoreConfiguration();
        storageServiceDataStoreConfiguration.setServerName(SERVER_NAME);
        storageServiceDataStoreConfiguration.setPort(SERVER_PORT);

        storageServiceDataStore = new StorageServiceDataStore(storageServiceDataStoreConfiguration);
    }

    @Test
    public void testStoreByteArray() throws DataStoreException, IOException {
        validateReferenceContainsExpected(storageServiceDataStore.store(TEST_STRING.getBytes(StandardCharsets.UTF_8), CONTAINER_ID));
    }

    @Test
    public void testStoreStream() throws DataStoreException, IOException {
        validateReferenceContainsExpected(storageServiceDataStore.store(new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8)), CONTAINER_ID));
    }

    @Test
    public void testStorePath() throws DataStoreException, IOException {
        Path path = File.createTempFile("tmp", "tmp").toPath();
        Files.write(path, TEST_STRING.getBytes(StandardCharsets.UTF_8));

        validateReferenceContainsExpected(storageServiceDataStore.store(path, CONTAINER_ID));
    }

    void validateReferenceContainsExpected(String reference) throws DataStoreException, IOException{
        InputStream inputStream = storageServiceDataStore.retrieve(reference);
        String storedString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        assertEquals(TEST_STRING, storedString);
    }
}
