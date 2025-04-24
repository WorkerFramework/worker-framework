#### Version Number
${version-number}

#### New Features
- US1016047: Introduced `CAF_RABBITMQ_TLS_PROTOCOL_VERSION` environment variable so that when Rabbit MQ protocol is
set to "amqps" a TLS version can be specified.
    - By default, this variable is set to "TLSv1.2".    

#### Breaking Changes
- US1009117: Interfaces have been updated to support messages beyond a configured threshold. 
  - TaskCallback
  - WorkerQueueProvider

#### Known Issues
- None
