/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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
({
    url: getenv("CAF_WORKER_HTTP_DATASTORE_URL"),
    connectTimeoutMillis: getenv("CAF_WORKER_HTTP_DATASTORE_CONNECT_TIMEOUT_MILLIS") || undefined,
    readTimeoutMillis: getenv("CAF_WORKER_HTTP_DATASTORE_READ_TIMEOUT_MILLIS") || undefined
});
