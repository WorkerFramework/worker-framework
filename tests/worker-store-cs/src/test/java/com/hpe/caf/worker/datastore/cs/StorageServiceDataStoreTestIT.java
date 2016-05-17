package com.hpe.caf.worker.datastore.cs;

import com.google.common.base.Strings;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.storage.sdk.StorageClient;
import com.hpe.caf.storage.sdk.exceptions.StorageClientException;
import com.hpe.caf.storage.sdk.exceptions.StorageServiceConnectException;
import com.hpe.caf.storage.sdk.exceptions.StorageServiceException;
import com.hpe.caf.storage.sdk.model.AccessPermissionType;
import com.hpe.caf.storage.sdk.model.DelegationTicket;
import com.hpe.caf.storage.sdk.model.DelegationTicketInfo;
import com.hpe.caf.storage.sdk.model.requests.CreateDelegationTicketRequest;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
    private static final String TEST_STRING2 = " ং ঃ অ আ ই ঈ উ ঊ ঋ ঌ এ ঐ ও ঔ ক খ গ ঘ ঙ চ ছ জ ঝ ঞ ই ঈ উ ট ঠ";

    private static final String DELEGATION_TICKET_INFO_DELEGATE_LOGIN = "caf_store_bfs@groups.int.hpe.com";
    private static final String DELEGATION_TICKET_TEST = "delegation ticket request";

    StorageServiceDataStore storageServiceDataStore;
    StorageServiceDataStore storageServiceDataStoreWithMockKeycloakClient;

    KeycloakClient keycloakClient;
    StorageClient storageClient;

    String delegationTicketValue;

    @Before
    public void setUp() throws Exception {
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

        keycloakClient = new KeycloakClient(storageServiceDataStoreConfiguration.getAuthenticationConfiguration());
        storageServiceDataStore = new StorageServiceDataStore(storageServiceDataStoreConfiguration, keycloakClient);

        //  Used for testTokenRefresh test in order to force storage-sdk to request an updated access token.
        KeycloakClient mockKeycloakClient = Mockito.mock(KeycloakClient.class);
        Mockito.when(mockKeycloakClient.getAccessToken()).thenReturn("invalid access token");
        storageServiceDataStoreWithMockKeycloakClient = new StorageServiceDataStore(storageServiceDataStoreConfiguration, mockKeycloakClient);

        storageClient = new StorageClient(SERVER_NAME,String.valueOf(SERVER_PORT));

        //  Create a delegation ticket.
        try {
            final DelegationTicketInfo delegationTicketInfo = createDelegationTicketInfo();
            final CreateDelegationTicketRequest delegationTicketRequest =
                    createDelegationTicketRequest(keycloakClient.getAccessToken(),CONTAINER_ID, delegationTicketInfo);
            DelegationTicket delegationTicket = storageClient.createDelegationTicket(delegationTicketRequest);
            delegationTicketValue = delegationTicket.getDelegateTicket();

            //  Use test value for delegation ticket if returned empty.
            if (Strings.isNullOrEmpty(delegationTicketValue)) {
                delegationTicketValue = DELEGATION_TICKET_TEST;
            }

        } catch (final StorageClientException sce) {
            // StorageClientException indicates a problem within the CAF Storage SDK.
            System.err.println("Client error: " + sce.getMessage());
            throw sce;

        } catch (final StorageServiceConnectException ssce) {

            // StorageServiceConnectException indicates a problem contacting the CAF storage service
            System.err.println("Storage service connection error: " + ssce.getMessage());
            throw ssce;

        } catch (final StorageServiceException sse) {

            // StorageServiceException indicates a problem on the CAF Storage server.
            // If it is a 400-level HTTP error, the problem is with your request. You may be able
            // to proceed by correcting the parameters on your request and trying again.
            // If it is a 500-level HTTP error, the problem is with the server code itself. After
            // the server is fixed, you may be able to proceed by resubmitting your request.
            System.err.println("Server error (" + sse.getHTTPStatus() + "): " + sse.getMessage());
            throw sse;

        } catch (final Exception exc) {

            System.err.println("Unexpected error: " + exc.getMessage());
            throw exc;
        }


    }

    @Test
    public void testStoreByteArray() throws DataStoreException, IOException {
        validateReferenceContainsExpected(storageServiceDataStore.store(TEST_STRING.getBytes(StandardCharsets.UTF_8), CONTAINER_ID),TEST_STRING);
    }

    @Test
    public void testStoreStream() throws DataStoreException, IOException {
        validateReferenceContainsExpected(storageServiceDataStore.store(new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8)), CONTAINER_ID),TEST_STRING);
    }

    @Test
    public void testStorePath() throws DataStoreException, IOException {
        Path path = File.createTempFile("tmp", "tmp").toPath();
        Files.write(path, TEST_STRING.getBytes(StandardCharsets.UTF_8));

        validateReferenceContainsExpected(storageServiceDataStore.store(path, CONTAINER_ID), TEST_STRING);
    }

    @Test
    public void testTokenRefresh() throws Exception {
        Path path = File.createTempFile("tmp", "tmp").toPath();
        Files.write(path, TEST_STRING2.getBytes(StandardCharsets.UTF_8));

        //  This test will make initial call to CAF storage with an inavlid access token. It will force the storage-sdk
        //  to call into callback function to request an updated and valid access token which will then allow test to pass.
        validateReferenceContainsExpected(storageServiceDataStoreWithMockKeycloakClient.store(path, CONTAINER_ID), TEST_STRING2);
    }

    @Test
    public void testStoreWithDelegationTicket() throws Exception {
        try {
            Path path = File.createTempFile("tmp", "tmp").toPath();
            Files.write(path, TEST_STRING2.getBytes(StandardCharsets.UTF_8));

            String reference = CONTAINER_ID + "?delegationTicket=" + URLEncoder.encode(delegationTicketValue,"UTF-8");
            String cafStoreReference = storageServiceDataStore.store(path, reference);
            cafStoreReference = cafStoreReference + "?delegationTicket=" + URLEncoder.encode(delegationTicketValue,"UTF-8");
            validateReferenceContainsExpected(cafStoreReference, TEST_STRING2);
        } catch (final Exception e) {

            System.err.println("Unexpected error: " + e.getMessage());
            throw e;
        }
    }

    void validateReferenceContainsExpected(String reference, String testString) throws DataStoreException, IOException {
        InputStream inputStream = storageServiceDataStore.retrieve(reference);
        String storedString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        assertEquals(testString, storedString);
    }

    /**
     * Method for creating DelegationTicketInfo with predefined values.
     * @return DelegationTicketInfo
     */
    static DelegationTicketInfo createDelegationTicketInfo() {
        // Create list of Storage permissions, based on the operations to be performed
        // using the delegation ticket.
        // If any 'Storage permission' required for an operation is missing
        // in this list and if you attempt to use such a delegation ticket
        // for performing the operation, then you'll be denied access.

        final List<String> permissions = new ArrayList<>();
        permissions.add(AccessPermissionType.CREATE_ASSET.name());
        permissions.add(AccessPermissionType.UPDATE_ASSET.name());
        permissions.add(AccessPermissionType.VIEW_ASSETS.name());
        permissions.add(AccessPermissionType.RETRIEVE_ASSET.name());

        final DelegationTicketInfo delegationTicketInfo =
                new DelegationTicketInfo.Builder()
                        .delegateLogin(DELEGATION_TICKET_INFO_DELEGATE_LOGIN)
                        .lifetimeInSeconds(Long.valueOf(300))
                        .permissions(permissions)
                        .build();
        return delegationTicketInfo;
    }

    /**
     * Method for creating CreateDelegationTicketRequest (required for invoking createDelegationTicket() method of SDK.
     * @param accessToken: Access token of a user who has special permission 'CREATE_DELEGATION_TICKET permission of IDM' to create a delegation ticket.
     *                     The STORAGE_ROOT user does NOT normally have this permission.
     * @param containerId Id of container for which the delegation ticket is to be created.
     * @param delegationTicketInfo Information required for creating delegation ticket.
     * @return CreateDelegationTicketRequest object that can later be passed to createDelegationTicket() method of SDK.
     */
    static CreateDelegationTicketRequest
    createDelegationTicketRequest(String accessToken,
                                  String containerId,
                                  DelegationTicketInfo delegationTicketInfo) {
        final CreateDelegationTicketRequest request =
                new CreateDelegationTicketRequest(accessToken, containerId, delegationTicketInfo);
        return request;
    }

}
