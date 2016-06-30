package com.hpe.caf.worker.datastore.cs;

//  An interface implemented for keycloak access token refresh.
public interface TokenRefreshListener {
    void tokenRefreshed(String token);
}
