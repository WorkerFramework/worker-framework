!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- 792072: Improvement of poison message error description. 
  - Environment variable CAF_WORKER_FRIENDLY_NAME has been added to specify the worker referred to in poison message error. If not 
    defined, the worker class name will be used by default.

#### Bug Fixes
- 743080: Excessive calls to queueDeclare have been prevented by recording previously declared queues.

#### Known Issues
