package com.hpe.caf.worker.datastore.cs;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code KeycloakClient} class responsible for acquiring authentication token from Keycloak server.
 */
public class KeycloakClient {

    private static final Logger LOG = LoggerFactory.getLogger(KeycloakClient.class);

    private final KeycloakAuthenticationConfiguration configuration;

    /**
     * Instantiates a new Keycloak client.
     *
     * @param configuration the configuration
     */
    public KeycloakClient(final KeycloakAuthenticationConfiguration configuration) {

        this.configuration = configuration;
    }

    /**
     * Gets access token.
     *
     * @return the access token
     * @throws IOException if access token retrieval failed.
     */
    public String getAccessToken() throws IOException {

        try (CloseableHttpClient client = HttpClientBuilder.create().build()){

            HttpPost post = new HttpPost(
                    KeycloakUriBuilder.fromUri(String.format("https://%s:%d/auth", configuration.getServerName(), configuration.getPort()))
                            .path(ServiceUrlConstants.TOKEN_PATH).build(configuration.getRealm()));
            List<NameValuePair> formparams = new ArrayList<>();
            formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "password"));
            formparams.add(new BasicNameValuePair("username", configuration.getUserName()));
            formparams.add(new BasicNameValuePair("password", configuration.getPassword()));
            String authorization = BasicAuthHelper.createHeader(configuration.getClientName(), configuration.getClientSecret());
            post.setHeader("Authorization", authorization);
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);

            LOG.debug("About to request authentication token.");
            HttpResponse response = client.execute(post);
            int status = response.getStatusLine().getStatusCode();
            LOG.debug("Authentication token response received. Status code: " + status);
            HttpEntity entity = response.getEntity();
            if (status != 200) {
                String content = IOUtils.toString(entity.getContent());

                LOG.error("Failed to retrieve authentication token. Status code:" + status +". Response content: " + content);
                throw new IOException("Bad status: " + status + ". Content: " + content);
            }
            if (entity == null) {
                throw new IOException("No Entity");
            }

            try (InputStream is = entity.getContent()) {
                AccessTokenResponse tokenResponse = JsonSerialization.readValue(is, AccessTokenResponse.class);
                return tokenResponse.getToken();
            }
        }
    }
}
