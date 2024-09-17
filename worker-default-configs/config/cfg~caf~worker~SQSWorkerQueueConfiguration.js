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
    inputQueue: getenv("CAF_WORKER_INPUT_QUEUE")
        || (getenv("CAF_WORKER_BASE_QUEUE_NAME") || getenv("CAF_WORKER_NAME") || "worker") + "-in",
    pausedQueue: getenv("CAF_WORKER_PAUSED_QUEUE") || undefined,
    retryQueue: getenv("CAF_WORKER_RETRY_QUEUE") || undefined,
    rejectedQueue: "worker-rejected",
    longPollInterval: getenv("CAF_SQS_LONG_POLL_INTERVAL") || 0,
    maxNumberOfMessages: getenv("CAF_SQS_MAX_NUMER_OF_MESSAGES") || 10,
    visibilityTimeout: getenv("CAF_SQS_VISIBILITY_TIMEOUT") || 300,
    messageRetentionPeriod: getenv("CAF_SQS_MESSAGE_RETENTION_PERIOD") || 1209600,
    maxDeliveries: getenv("CAF_SQS_MAX_DELIVERIES") || 2,
    maxInflightMessages: getenv("CAF_SQS_MAX_INFLIGHT_MESSAGES") || 120000,
    publisherWaitTimeout: getenv("CAF_SQS_PUBLISHER_WAIT_TIMEOUT") || 5
});
