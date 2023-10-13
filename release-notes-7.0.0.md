!not-ready-for-release!

#### Version Number
${version-number}

#### New Features

#### Bug Fixes
- 743080: Excessive calls to queueDeclare have been prevented by recording previously declared queues.

#### Breaking Changes
- **US361030**: Java 8 and Java 11 support dropped  
  Java 17 is now the minimum supported version.

- **US361030**: BOM no longer supplied  
  The `worker-framework` BOM project module is not longer supplied.

- **US361030**: Jakarta EE version update  
  The version of Jakarta EE used for validation and other purposes has been updated
  from Jakarta EE 8 to Jakarta EE 9.  This may mean that `javax.*` imports in worker
  code need to be updated to `jakarta.*` instead.

#### Known Issues
