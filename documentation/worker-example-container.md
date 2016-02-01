# worker-example-container
---
This is a docker container for the Example Worker. It consists of the Example Worker
which can be run by passing in the required configuration files to the
container. It uses 'java:8' as a base image.

## Configuration
### Configuration Files
The worker requires configuration files to be passed through for:

* ExampleWorkerConfiguration
* RabbitWorkerQueueConfiguration
* StorageServiceDataStoreConfiguration

### Environment Variables
##### CAF\_CONFIG\_PATH
The location of the configuration files to be used by the worker.
Common to all workers.
