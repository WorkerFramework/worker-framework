#### Version Number
${version-number}

#### New Features
- SCMOD-12257: Correlation Id support has been added to worker-framework which can be used for tracking messages.  
The Correlation Id will be included in the output message so downstream workers must be on a version that supports it.

#### Bug Fixes
- SCMOD-13301: Removed any use of Files.exists()  
java.io.File.exists sometimes reports incorrect result ([JDK-5003595](https://bugs.java.com/bugdatabase/view_bug.do?bug_id=5003595))
- SCMOD-12319: The worker archetype has been deprecated.

#### Breaking Changes
- SCMOD-12730: Added pause task functionality.
  - The `statusCheckUrl` workers used to check the status of a task now points to the `status` endpoint instead of the 
  `isActive` endpoint.
  - Instead of returning `true` or `false`, the `statusCheckUrl` will now return one of `Active`, `Cancelled`, 
  `Completed`, `Failed`, `Paused`, or `Waiting`.
  - When a worker receives a task, it will now check if the task has been paused using the `statusCheckUrl`.
  - If the task has been paused, and the `CAF_WORKER_PAUSED_QUEUE` environment variable is set, the worker will publish 
  the task to the `CAF_WORKER_PAUSED_QUEUE` instead of processing it.
  - If the task has been paused, and the `CAF_WORKER_PAUSED_QUEUE` environment variable is NOT set, the worker process 
  the task as normal (as if the task was not paused).

#### Known Issues
- None
