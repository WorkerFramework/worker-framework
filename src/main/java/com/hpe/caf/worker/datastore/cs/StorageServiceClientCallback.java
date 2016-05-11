package com.hpe.caf.worker.datastore.cs;

import com.hpe.caf.storage.sdk.StorageClientCallback;
import com.hpe.caf.storage.sdk.exceptions.StorageClientException;
import com.hpe.caf.storage.sdk.exceptions.StorageServiceConnectException;
import com.hpe.caf.storage.sdk.exceptions.StorageServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StorageServiceClientCallback implements StorageClientCallback {

    private static final Logger LOG = LoggerFactory.getLogger(StorageServiceClientCallback.class);

    private final KeycloakClient keycloakClient;

    public StorageServiceClientCallback (final StorageServiceDataStoreConfiguration storageServiceDataStoreConfiguration) {
        keycloakClient = storageServiceDataStoreConfiguration.getAuthenticationConfiguration() != null ? new KeycloakClient(storageServiceDataStoreConfiguration.getAuthenticationConfiguration()) : null;
    }

    @Override
    public String refreshToken() throws StorageClientException, StorageServiceException, StorageServiceConnectException {

        String accessToken = null;

        //  Supply an updated access token in the event that it expires in the middle of processing an asset.
        //  The storage SDK will call this method to get the updated token and continue working on the asset in progress.
        if (keycloakClient != null) {
            try {
                LOG.debug("About to request access token.");
                accessToken = keycloakClient.getAccessToken();
            } catch (IOException e) {
                LOG.error("Failed to retrieve access token.");
                e.printStackTrace();
            }
        }

        return accessToken;
    }
}
