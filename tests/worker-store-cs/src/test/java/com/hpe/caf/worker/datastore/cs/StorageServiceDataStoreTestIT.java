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

    private static final String SERVER_NAME = "a1-dev-mem031.lab.lynx-connected.com";
    private static final int SERVER_PORT = 9444;
    private static final String AUTH_CONFIG_SERVER_NAME = "a1-dev-hap045.lab.lynx-connected.com";
    private static final int AUTH_CONFIG_PORT = 8443;
    private static final String AUTH_CONFIG_USERNAME = "caf_store_bfs@groups.int.hpe.com";
    private static final String AUTH_CONFIG_PASSWORD = "Password1";
    private static final String AUTH_CONFIG_CLIENT_NAME = "direct-grant-client";
    private static final String AUTH_CONFIG_CLIENT_SECRET = "34a868ed-b2a3-4433-bca3-d60cabcd79df";
    private static final String AUTH_CONFIG_REALM = "caf";
    private static final String CONTAINER_ID = "c82335049236404ba86529e9afacba39";

    private static final String TEST_STRING = " ং ঃ অ আ ই ঈ উ ঊ ঋ ঌ এ ঐ ও ঔ ক খ গ ঘ ঙ চ ছ জ ঝ ঞ ট ঠ";

    StorageServiceDataStore storageServiceDataStore;

    @Before
    public void setUp(){
        StorageServiceDataStoreConfiguration storageServiceDataStoreConfiguration = new StorageServiceDataStoreConfiguration();
        storageServiceDataStoreConfiguration.setServerName(SERVER_NAME);
        storageServiceDataStoreConfiguration.setPort(SERVER_PORT);

        KeycloakAuthenticationConfiguration keycloakAuthenticationConfiguration = new KeycloakAuthenticationConfiguration();
        keycloakAuthenticationConfiguration.setClientName(AUTH_CONFIG_CLIENT_NAME);
        keycloakAuthenticationConfiguration.setClientSecret(AUTH_CONFIG_CLIENT_SECRET);
        keycloakAuthenticationConfiguration.setPassword(AUTH_CONFIG_PASSWORD);
        keycloakAuthenticationConfiguration.setPort(AUTH_CONFIG_PORT);
        keycloakAuthenticationConfiguration.setServerName(AUTH_CONFIG_SERVER_NAME);
        keycloakAuthenticationConfiguration.setRealm(AUTH_CONFIG_REALM);
        keycloakAuthenticationConfiguration.setUserName(AUTH_CONFIG_USERNAME);
        storageServiceDataStoreConfiguration.setAuthenticationConfiguration(keycloakAuthenticationConfiguration);

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
