!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- None

#### Bug Fixes
- D1059209: Fixed an issue where poison messages was not being processed correctly for Bulk Workers.
  - The `CAF_WORKER_RETRY_LIMIT` environment variable is now respected for Bulk Workers
  - A position message will now be sent to the reject queue after exceeding the retry limit, and an exception
    response will be set on the worker.

#### Known Issues
- None
