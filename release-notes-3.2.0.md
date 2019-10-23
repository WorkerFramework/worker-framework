#### Version Number
${version-number}

#### New Features
* SCMOD-7470: Support disabling bootstrapping Logback  
  Bootstrapping is based on whether or not a logback.xml file is present.

#### Bug Fixes
* SCMOD-5410: Potential for task statuses not to be reported.  
  An unexpected termination of a worker could result in the loss of job tracking messages.
* SCMOD-7272: Updated the publish confirm count for retry queue to reflect correct state
