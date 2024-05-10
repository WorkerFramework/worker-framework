!not-ready-for-release!

#### Version Number
${version-number}

#### Breaking Changes
- US915147: Liveness and readiness check support has been added to the `WorkerFactory` interface.  
  - The `healthCheck` method has been removed from the `WorkerFactory` interface, and replaced by new `checkAlive` and `checkReady`methods.
  - See the [documentation](https://github.com/WorkerFramework/worker-framework/tree/develop/worker-core#liveness-and-readiness-checks-within-the-worker-framework)
    for more details.

#### New Features
- US915147: New liveness and readiness endpoints added.   
  - A new `/health-check?name=all&type=ALIVE` endpoint has been added on the default REST port (8080) to check if a worker is alive
  - A new `/health-check?name=all&type=READY` endpoint has been added on the default REST port (8080) to check if a worker is ready
  - See the [documentation](https://github.com/WorkerFramework/worker-framework/tree/develop/worker-core#liveness-and-readiness-checks-within-the-worker-framework)
    for more details.

#### Known Issues
- None
