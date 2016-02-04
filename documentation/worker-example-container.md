# worker-example-container
---
This is a docker container for the Example Worker. It consists of the Example Worker in the build sub-module and the integration
tests found in the test sub-module. The build sub-module is responsible for building the worker image and pushing to docker.
The test sub-module is responsible for starting the worker container, RabbitMQ container and assembling the configuration files
in the test-configs folder.
It uses 'java:8' as a base image.

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
##### CAF\_APPNAME
The name of the worker. Common to all workers.