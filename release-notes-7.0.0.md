!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- 792072: Improvement of poison message error description. 
  - Environment variable CAF_WORKER_FRIENDLY_NAME has been added to specify the worker referred to in poison message error. If not 
    defined, the worker class name will be used by default.

#### Bug Fixes
- 743080: Excessive calls to queueDeclare have been prevented by recording previously declared queues.

#### Breaking Changes
- **US361030**: Java 8 and Java 11 support dropped  
  Java 17 is now the minimum supported version.

- **US361030**: BOM no longer supplied  
  The `worker-framework` BOM project module is no longer supplied.

- **US361030**: Jakarta EE version update  
  The version of Jakarta EE used for validation and other purposes has been updated
  from Jakarta EE 8 to Jakarta EE 9.  This may mean that `javax.*` imports in worker
  code need to be updated to `jakarta.*` instead.

- **I854021**: Reinstate base64 encoding of taskData
  TaskData is once again encoded using base64 and the V4 message format no longer supported.

#### Known Issues
