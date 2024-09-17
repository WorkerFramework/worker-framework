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
    sqsProtocol: getenv("CAF_SQS_PROTOCOL") || "http",
    sqsHost: getenv("CAF_SQS_HOST") || "localhost",
    sqsPort: getenv("CAF_SQS_PORT") || 4566,
    sqsRegion: getenv("CAF_SQS_REGION") || "us-east-1",
    sqsAccessKey: getenv("CAF_SQS_ACCESS_KEY") || "x",
    sqsSecretAccessKey: getenv("CAF_SQS_SECRET_ACCESS_KEY") || "x"
});
