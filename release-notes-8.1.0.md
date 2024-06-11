#### Version Number
${version-number}

#### New Features
- **US914108:** Version Currency: JUnit 5 migration
- **US915147:** Support for liveness and readiness checks added.
-   - The `WorkerFactory` interface contains a new `livenessCheck` method, which has a default implementation that returns
      `HealthResult.RESULT_HEALTHY`. A worker may optionally override this method to provide their own implementation of liveness.
- A new `/health-check?name=all&type=ALIVE` endpoint has been added on the default REST port (8080) to check if a worker is alive
- A new `/health-check?name=all&type=READY` endpoint has been added on the default REST port (8080) to check if a worker is ready
- See the [documentation](https://github.com/WorkerFramework/worker-framework/tree/develop/worker-core#health-checks-within-the-worker-framework)
  for more details.

#### Bug Fixes
- **I506009:** Fix implemented to ensure that adjusting CAF_WORKER_RETRY_LIMIT to the current retry value will  
               no longer result in infinite retries of a message. The message will now be correctly identified as
               poisonous and passed on to the next worker.

#### Known Issues
- None