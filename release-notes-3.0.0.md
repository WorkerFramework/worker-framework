#### Version Number
${version-number}

#### New Features
 - Added support for asynchronous job updates
    Previously messages were proxied through the Job Tracking worker and it updated the database with their progress. This made the worker too much of a bottleneck. Instead the Worker Framework now sends the progress messages directly to the Job Tracking worker and then separately sends the output message to the target.
     
#### Breaking Changes
 - As a result of adding support for asynchronous job updates, Job Service 2.3.0 and versions prior are no longer supported.
- [SCMOD-4072](https://jira.autonomy.com/browse/SCMOD-4072): Updated to use latest version of dropwizard ([1.3.2](https://github.com/dropwizard/dropwizard/tree/v1.3.2)).
    
#### Known Issues
 - None