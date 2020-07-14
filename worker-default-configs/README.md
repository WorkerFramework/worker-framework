# Worker Default Configs

This project contains a set of default JavaScript configuration files that can be used with a standard worker framework worker. Each configuration property checks an environment variables for a value and if no value is found for any environment variable a default value is set if applicable.

## FileSystemDataStoreConfiguration

The default FileSystemDataStore configuration file checks for values as below;

| Property | Checked Environment Variables | Default               |
|----------|-------------------------------|-----------------------|
| dataDir  |  `CAF_WORKER_DATASTORE_PATH` | /mnt/caf-datastore-root  |
| dataDirHealthcheckTimeoutSeconds  |  `CAF_WORKER_DATASTORE_HEALTHCHECK_TIMEOUT_SECONDS` | 10  |


## RabbitConfiguration

The default Rabbit configuration file checks for values as below;

| Property | Checked Environment Variables | Default               |
|----------|-------------------------------|-----------------------|
| backoffInterval  |  `CAF_RABBITMQ_BACKOFF_INTERVAL` | 5  |
| maxBackoffInterval  |  `CAF_RABBITMQ_MAX_BACKOFF_INTERVAL` | 15  |
| maxAttempts  |  `CAF_RABBITMQ_MAX_ATTEMPTS` | 3  |
| rabbitHost  |  `CAF_RABBITMQ_HOST` | rabbitmq  |
| rabbitPort  |  `CAF_RABBITMQ_PORT` | 5672  |
| rabbitUser  |  `CAF_RABBITMQ_USERNAME` | guest  |
| rabbitPassword  |  `CAF_RABBITMQ_PASSWORD` | guest  |

## RabbitWorkerQueueConfiguration

The default RabbitWorkerQueue configuration file checks for values as below;

| Property | Checked Environment Variables | Default               |
|----------|-------------------------------|-----------------------|
| prefetchBuffer  |  `CAF_RABBITMQ_PREFETCH_BUFFER` | 1  |
| inputQueue  |  `CAF_WORKER_INPUT_QUEUE` | worker-in  |
|             |  `CAF_WORKER_BASE_QUEUE_NAME` with '-in' appended to the value if present    |    |
|             |  `CAF_WORKER_NAME` with '-in' appended to the value if present        |    |
| retryQueue  |  `CAF_WORKER_RETRY_QUEUE` |   |
| rejectedQueue  |   | worker-rejected  |
| retryLimit  |  `CAF_WORKER_RETRY_LIMIT` | 10  |
