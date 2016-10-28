package com.hpe.caf.worker.datastore.cs;

import com.hpe.caf.storage.sdk.StorageClientCallback;
import com.hpe.caf.storage.sdk.exceptions.StorageClientException;
import com.hpe.caf.storage.sdk.exceptions.StorageServiceConnectException;
import com.hpe.caf.storage.sdk.exceptions.StorageServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StorageServiceClientCallback implements StorageClientCallback {

    private static final Logger LOG = LoggerFactory.getLogger(StorageServiceClientCallback.class);

    private List<TokenRefreshListener> listeners = new ArrayList<TokenRefreshListener>();

    private final KeycloakClient keycloakClient;

    private String accessToken = "";

    private final Object syncLock = new Object();

    public StorageServiceClientCallback (final StorageServiceDataStoreConfiguration storageServiceDataStoreConfiguration) {
        keycloakClient = storageServiceDataStoreConfiguration.getAuthenticationConfiguration() != null ? new KeycloakClient(storageServiceDataStoreConfiguration.getAuthenticationConfiguration()) : null;
    }

    public void addTokenRefreshListener(TokenRefreshListener toAdd) {
        listeners.add(toAdd);
    }

    @Override
    public String refreshToken() throws StorageClientException, StorageServiceException, StorageServiceConnectException {

        String currentAccessToken = new String(accessToken);

        synchronized (syncLock) {

            //  If access token has already been refreshed then return refreshed token value.
            if(!currentAccessToken.equals(accessToken)){
                return accessToken;
            }

            //  Supply an updated access token in the event that it expires in the middle of processing an asset.
            //  The storage SDK will call this method to get the updated token and continue working on the asset in progress.
            if (keycloakClient != null) {
                try {
                    LOG.debug("About to request access token.");
                    accessToken = keycloakClient.getAccessToken();

                    // Notify all listeners that the access token has been refreshed.
                    for (TokenRefreshListener rtl : listeners)
                        rtl.tokenRefreshed(accessToken);

                } catch (IOException e) {
                    LOG.error("Failed to retrieve access token.");
                    throw new StorageClientException("Failed to retrieve access token.", e);
                }
            }

        }

        return accessToken;
    }
}
