!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- None

#### Breaking Changes
- US915147: Removal of `/healthcheck` endpoint.  
  The `/healthcheck` endpoint has been removed, and has been replaced by liveness and readiness endpoints:
  - `/health-check?name=all&type=ALIVE`  
  - `/health-check?name=all&type=READY`  

  See the [documentation](https://github.com/WorkerFramework/worker-framework/tree/develop/worker-core#health-checks-within-the-worker-framework) 
  for more details on these new endpoints.

#### Known Issues
- None
