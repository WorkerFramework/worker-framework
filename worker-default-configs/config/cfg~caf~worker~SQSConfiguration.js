/*
 * Copyright 2015-2024 Open Text.
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
    awsProtocol: getenv("CAF_AWS_PROTOCOL") || "http",
    awsHost: getenv("CAF_AWS_HOST") || "localhost",
    awsPort: getenv("CAF_AWS_PORT") || 4566,
    awsRegion: getenv("CAF_AWS_REGION") || "us-east-1",
    awsAccessKey: getenv("CAF_AWS_ACCESS_KEY") || "x",
    awsSecretAccessKey: getenv("CAF_AWS_SECRET_ACCESS_KEY") || "x"
});
