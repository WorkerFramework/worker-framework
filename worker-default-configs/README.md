# Worker Default Configs

This project contains a set of default JavaScript configuration files that can be used with a standard worker framework worker. Each configuration property checks an environment variables for a value and if no value is found for any environment variable a default value is set if applicable.

## FileSystemDataStoreConfiguration

The default FileSystemDataStore configuration file checks for values as below;

| Property | Checked Environment Variables | Default               |
|----------|-------------------------------|-----------------------|
| dataDir  |  `CAF_WORKER_DATASTORE_PATH` | /mnt/caf-datastore-root  |
| dataDirHealthcheckTimeoutSeconds  |  `CAF_WORKER_DATASTORE_HEALTHCHECK_TIMEOUT_SECONDS` | 10  |

## HttpDataStoreConfiguration

The HttpDataStore configuration file checks for values as below;

| Property | Checked Environment Variables | Default               |
|----------|-------------------------------|-----------------------|
| url  |  `CAF_WORKER_HTTP_DATASTORE_URL` | undefined  |
| connectTimeoutMillis  |  `CAF_WORKER_HTTP_DATASTORE_CONNECT_TIMEOUT_MILLIS` | 10000  |
| readTimeoutMillis  |  `CAF_WORKER_HTTP_DATASTORE_READ_TIMEOUT_MILLIS` | 10000  |

## RabbitConfiguration

The default Rabbit configuration file checks for values as below;

| Property           | Checked Environment Variables       | Default  |
|--------------------|-------------------------------------|----------|
| backoffInterval    | `CAF_RABBITMQ_BACKOFF_INTERVAL`     | 5        |
| maxBackoffInterval | `CAF_RABBITMQ_MAX_BACKOFF_INTERVAL` | 15       |
| maxAttempts        | `CAF_RABBITMQ_MAX_ATTEMPTS`         | 3        |
| rabbitProtocol     | `CAF_RABBITMQ_PROTOCOL`             | amqp     |
| rabbitHost         | `CAF_RABBITMQ_HOST`                 | rabbitmq |
| rabbitPort         | `CAF_RABBITMQ_PORT`                 | 5672     |
| rabbitUser         | `CAF_RABBITMQ_USERNAME`             | guest    |
| rabbitPassword     | `CAF_RABBITMQ_PASSWORD`             | guest    |

## RabbitWorkerQueueConfiguration

The default RabbitWorkerQueue configuration file checks for values as below;

| Property | Checked Environment Variables | Default               |
|----------|-------------------------------|-----------------------|
| prefetchBuffer  |  `CAF_RABBITMQ_PREFETCH_BUFFER` | 1  |
| inputQueue  |  `CAF_WORKER_INPUT_QUEUE` | worker-in  |
|             |  `CAF_WORKER_BASE_QUEUE_NAME` with '-in' appended to the value if present    |    |
|             |  `CAF_WORKER_NAME` with '-in' appended to the value if present        |    |
| pausedQueue  |  `CAF_WORKER_PAUSED_QUEUE` |   |
| retryQueue  |  `CAF_WORKER_RETRY_QUEUE` |   |
| rejectedQueue  |   | worker-rejected  |
| retryLimit  |  `CAF_WORKER_RETRY_LIMIT` | 10  |

## MessageSystemConfiguration

The default MessageSystemConfiguration configuration file checks for values as below;

Currently supported messaging system implementations:
- Rabbit MQ -> value "rabbitmq"
- AWS SQS   -> value "sqs"

| Property       | Checked Environment Variables    | Default  |
|----------------|----------------------------------|----------|
| implementation | `CAF_MESSAGING_IMPLEMENTATION`   | rabbitmq |

## SQSConfiguration

The default SQS configuration file checks for values as below;

| Property           | Checked Environment Variables | Default   |
|--------------------|-------------------------------|-----------|
| awsProtocol        | `CAF_SQS_PROTOCOL`            | http      |
| awsHost            | `CAF_SQS_HOST`                | localhost |
| awsPort            | `CAF_SQS_PORT`                | 5672      |
| awsRegion          | `CAF_SQS_REGION`              | guest     |
| awsAccessKey       | `CAF_SQS_ACCESS_KEY`          | guest     |
| awsSecretAccessKey | `CAF_SQS_SECRET_ACCESS_KEY`   | guest     |

## SQSWorkerQueueConfiguration

The default SQSWorkerQueue configuration file checks for values as below;

| Property               | Checked Environment Variables                                            | Default    |
|------------------------|--------------------------------------------------------------------------|------------|
| inputQueue             | `CAF_WORKER_INPUT_QUEUE`                                                 | worker-in  |
|                        | `CAF_WORKER_BASE_QUEUE_NAME` with '-in' appended to the value if present |            |
|                        | `CAF_WORKER_NAME` with '-in' appended to the value if present            |            |
| pausedQueue            | `CAF_WORKER_PAUSED_QUEUE`                                                | 1          |
| retryQueue             | `CAF_WORKER_RETRY_QUEUE`                                                 | 1          |
| rejectedQueue          | `worker-rejected`                                                        | 1          |
| longPollInterval       | `CAF_SQS_LONG_POLL_INTERVAL`                                             | 1          |
| maxNumberOfMessages    | `CAF_SQS_MAX_NUMER_OF_MESSAGES`                                          | 1          |
| visibilityTimeout      | `CAF_SQS_VISIBILITY_TIMEOUT`                                             | 1          |
| messageRetentionPeriod | `CAF_SQS_MESSAGE_RETENTION_PERIOD`                                       | 1          |
| maxDeliveries          | `CAF_SQS_MAX_DELIVERIES`                                                 | 1          |
| maxInflightMessages    | `CAF_SQS_MAX_INFLIGHT_MESSAGES`                                          | 120000     |
| publisherWaitTimeout   | `CAF_SQS_PUBLISHER_WAIT_TIMEOUT`                                         | 0          |

## HealthConfiguration

The default Heath configuration file checks for values as below;

| Property                         | Checked Environment Variables                 | Default |
|----------------------------------|-----------------------------------------------|---------|
| livenessInitialDelaySeconds      | `CAF_LIVENESS_INITIAL_DELAY_SECONDS`          | 15      |
| livenessCheckIntervalSeconds     | `CAF_LIVENESS_CHECK_INTERVAL_SECONDS`         | 60      |
| livenessDowntimeIntervalSeconds  | `CAF_LIVENESS_DOWNTIME_INTERVAL_SECONDS`      | 60      |
| livenessSuccessAttempts          | `CAF_LIVENESS_SUCCESS_ATTEMPTS`               | 1       |
| livenessFailureAttempts          | `CAF_LIVENESS_FAILURE_ATTEMPTS`               | 3       |
| readinessInitialDelaySeconds     | `CAF_READINESS_INITIAL_DELAY_SECONDS`         | 15      |
| readinessCheckIntervalSeconds    | `CAF_READINESS_CHECK_INTERVAL_SECONDS`        | 60      |
| readinessDowntimeIntervalSeconds | `CAF_READINESS_DOWNTIME_INTERVAL_SECONDS`     | 60      |
| readinessSuccessAttempts         | `CAF_READINESS_SUCCESS_ATTEMPTS`              | 1       |
| readinessFailureAttempts         | `CAF_READINESS_FAILURE_ATTEMPTS`              | 3       |
