/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
    prefetchBuffer: getenv("CAF_RABBITMQ_PREFETCH_BUFFER") || 1,
    inputQueue: getenv("CAF_WORKER_INPUT_QUEUE")
            || (getenv("CAF_WORKER_BASE_QUEUE_NAME") || getenv("CAF_WORKER_NAME") || "worker") + "-in",
    retryQueue: getenv("CAF_WORKER_RETRY_QUEUE") || undefined,
    rejectedQueue: "worker-rejected",
    retryLimit: getenv("CAF_WORKER_RETRY_LIMIT") || 10,
    maxPriority: getenv("CAF_RABBITMQ_MAX_PRIORITY") || 0
});
