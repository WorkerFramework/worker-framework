# worker-example

This code includes an implementation of a simple example worker for the CAF Worker framework. It performs
a text conversion on a text file based on an `Action` enumeration passed in from the [ExampleWorkerTask](https://github.com/WorkerFramework/worker-framework/blob/develop/worker-example/worker-example-shared/src/main/java/com/hpe/caf/worker/example/ExampleWorkerTask.java) class. It retrieves
the text from a data source and returns a result message containing either a reference to the result in the `DataStore` or
the result itself.


## General operation overview

The Worker will take a single input message with a reference to a text file data accessed via a `DataStore`. The result
will then be made available either directly as a byte array or as a reference via the `DataStore`.


## Configuration

This Worker uses the standard `caf-api` system of `ConfigurationSource` only.
The configuration class is [ExampleWorkerConfiguration](https://github.com/WorkerFramework/worker-framework/blob/develop/worker-example/worker-example/src/main/java/com/hpe/caf/worker/example/ExampleWorkerConfiguration.java), which has several options:

- `workerVersion`: the version number of the worker
- `outputQueue`: the name of the queue to put results upon
- `threads`: the number of threads to be used to host this Worker
- `resultSizeThreshold`: the result size limit (in bytes) at which the result
will be written to the DataStore rather than held in a byte array


## Input message (task) format

The task class is [ExampleWorkerTask](https://github.com/WorkerFramework/worker-framework/blob/develop/worker-example/worker-example-shared/src/main/java/com/hpe/caf/worker/example/ExampleWorkerTask.java) and has the following entries:

- `sourceData` (required): a reference to the data accessible either directly
or via the `DataStore`.
- `datastorePartialReference` (optional): the location within the DataStore
relative to which data will be stored.
- `action` (required): an enumeration determining the method of text manipulation which will be taken by the worker.


## Output message (result) format

The result class is [ExampleWorkerResult](https://github.com/WorkerFramework/worker-framework/blob/develop/worker-example/worker-example-shared/src/main/java/com/hpe/caf/worker/example/ExampleWorkerResult.java) and has the following entries:

- `workerStatus` (always present): processing result status. Any value other
than `COMPLETED` means failure. Failure means other entries will not be set.
Status can have one of the following values:
    - `COMPLETED`: task completed **successfully**
    - `SOURCE_FAILED`: the source data could not be acquired from the DataStore
    - `STORE_FAILED`: failed to store the OCR result in the `DataStore`
    - `WORKER_EXAMPLE_FAILED`: the input file could be read but the worker failed in an unexpected way
- `textData`: a reference to the result data in the DataStore or the data itself.


## Health checks

This Worker provides a basic health check. It creates an `ExampleWorkerFactoryProvider` object using the `ModuleLoader`.
If the call is successful this indicates that the module loader can retrieve the implementation and the health check will
return success.


## Resource usage

The number of Worker threads is configured using the configuration class
`ExampleWorkerConfiguration` member `threads`.

Memory usage will vary significantly with the size of the input file.

Any result whose size exceeds the `resultSizeThreshold` stipulated in the configuration class `ExampleWorkerConfiguration`
will be written to the DataStore rather than being held in a byte array directly within the worker result.


## Failure modes

The main points of failure for `worker-example` are:

- `Configuration errors`: these will manifest on startup and cause the worker
to fail to start. Check the logs for clues, and double check your configuration files.
- `DataStore errors`: for failure results with `SOURCE_FAILED` or `STORE_FAILED` status, you should check your configuration
for the `DataStore` and the connectivity and health of the store itself.
- `IO errors`: for results with `WORKER_EXAMPLE_FAILED`, this could be caused by the InputStream to String conversion, and
you should check that the input data folder is correctly referenced, is not empty, and contains valid UTF-8 characters.


## Upgrade procedures

These follow standard CAF Worker upgrade procedures. Note that if the version
of `worker-example-shared` has not changed then an upgrade to `worker-example` is an in-place upgrade.

If you need to do a rolling upgrade when `worker-example-shared` has changed
then:

- Spin up containers of the new version of `worker-example`
- Replace old versions of producers of `ExampleWorkerTask` with new ones
- Allow the queue with the old versions of `ExampleWorkerTask` to drain and
then shut down the old Worker containers


## Maintainers

 The following people are contacts for developing and maintaining this module:

 - Conal Smith (Belfast, UK, conal.smith@hpe.com)
 - Dermot Hardy (Belfast, UK, dermot.hardy@hpe.com)
 - Krzystof Ploch (Belfast, UK, krzysztof.ploch@hpe.com)
