/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function () {
    var storageConfig = {
        serverName: getenv("CAF_STORAGE_HOST") || "caf-storage",
        port: getenv("CAF_STORAGE_PORT") || 9110
    };

    var keycloakServer = getenv("CAF_STORAGE_KEYCLOAK_HOST") || getenv("CAF_KEYCLOAK_HOST");
    if (keycloakServer) {
        storageConfig.authenticationConfiguration = {
            serverName: keycloakServer,
            port: getenv("CAF_STORAGE_KEYCLOAK_PORT") || getenv("CAF_KEYCLOAK_PORT") || 9020,
            userName: getenv("CAF_STORAGE_USERNAME") || undefined,
            password: getenv("CAF_STORAGE_PASSWORD") || undefined,
            clientName: getenv("CAF_STORAGE_CLIENT_NAME") || "CAF_App",
            clientSecret: getenv("CAF_STORAGE_CLIENT_SECRET") || undefined,
            realm: getenv("CAF_STORAGE_KEYCLOAK_REALM") || getenv("CAF_KEYCLOAK_REALM") || "caf"
        };
    }

    return storageConfig;
})();
