
#### Version Number
${version-number}

#### New Features

* Worker's experiencing a transient failure will stop consuming tasks from their queues until the Health Checks all return healthy.
* Closed source dependencies removed from the project.

#### Breaking Changes

* Worker-Store-CS has been removed from Worker-Framework. Any workers relying on CAF-Storage should be updated to use FS-Storage. See [here](https://github.com/WorkerFramework/worker-framework/tree/develop/worker-store-fs) for more detail.
* Standard-Worker-Container has been updated to use **worker-store-fs** instead of worker-store-cs, workers or projects using with a dependency of standard-worker-container should ensure they are using FS-Storage.

#### Known Issues
