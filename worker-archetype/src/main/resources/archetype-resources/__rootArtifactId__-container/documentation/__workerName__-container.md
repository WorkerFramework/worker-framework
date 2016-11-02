# ${workerName}-container
---
This is a docker container for the ${workerName}. It consists of two sub modules, `build` and `test`. The `build` sub-module
is responsible for building the worker image and pushing it to docker. The ${workerName} uses 'java:8' as a base image. The
`test` sub-module is responsible for starting containers for the worker and RabbitMQ, assembling the configuration files in the
test-configs folder, and running the integration tests.

## Configuration
### Configuration Files
The worker requires configuration files to be passed through for:

* ${workerName}Configuration
* RabbitWorkerQueueConfiguration
* StorageServiceDataStoreConfiguration

### Environment Variables
##### CAF\_CONFIG\_PATH
The location of the configuration files to be used by the worker.
Common to all workers.
##### CAF\_APPNAME
The name of the worker. Common to all workers.