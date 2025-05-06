#### Version Number
${version-number}

#### Breaking Changes
- US1009117: Remove reliance on large messages being supported by the underlying queue provider.
  - The TaskCallback interface has been updated to expect a TaskMessage in place of a byte array.
  - The WorkerQueueProvider interface has been updated to expect a ManagedDataStore for storing large messages
    and a Codec for serialization/deserialization of messages prior to storage/retrieval from the datastore.

#### New Features
- US1016047: Introduced `CAF_RABBITMQ_TLS_PROTOCOL_VERSION` environment variable so that when Rabbit MQ protocol is
set to "amqps" a TLS version can be specified.
    - By default, this variable is set to "TLSv1.2".    

#### Known Issues
- None
