#### Version Number
${version-number}

#### Breaking Changes
- US1009117: Remove reliance on large messages being supported by the underlying queue provider.
  - The TaskCallback interface has been updated to expect a TaskMessage in place of a byte array.
  - The WorkerQueueProvider interface has been updated to expect a ManagedDataStore for storing large messages
    and a Codec for serialization/deserialization of messages prior to storage/retrieval from the datastore.

#### New Features
- None    

#### Known Issues
- None
