#### Version Number
${version-number}

#### New Features
 - **SCMOD-9780**: Java 11 support  
    The Worker Framework has been verified using Java 11 at runtime.  The archetype has been updated to use the latest released Java 11 image.

 - **SCMOD-8463**: Confirm health checks before starting  
    In version 3.0.0 the Worker Framework was updated to stop accepting work if any of its health checks started to fail.  This has now been updated again so that it will not start accepting work until there has been a successful pass of the health checks.

 - **SCMOD-9102**: New filesystem health check  
    A new health check has been added for confirming that the filesystem is accessible (when using the default `worker-store-fs` module).

 - **SCMOD-4887**: Poison message recording  
    Poison messages are now output to the worker's `reject` queue, to help to analyse why they caused the worker to crash.

#### Bug Fixes
 - None

#### Known Issues
 - None
