!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- **US857114:** Quorum queues leveraging 'x-delivery-count' for the handling of poison messages.

#### Bug Fixes
- **I445035**: Workers now attempt to complete all in-progress and pre-fetched tasks before shutting down.
- **I874162**: Fixed issue where the `JobStatusResponseCache` was not working for HTTPS requests.

#### Known Issues

#### Breaking Changes
- **US749035**: Classic queues and priority queues are deprecated, priority queues can still be created however it is no
  longer possible to publish messages with a priority.
  When using `cfg~caf~worker~RabbitWorkerQueueConfiguration.js`, the type of queue created by workers can be controlled 
  by the ENV CAF_RABBITMQ_QUEUE_TYPE, this currently defaults to 'quorum', but will be removed in a future release.
- **US857114**: Introduced configurable Rabbit MQ protocol so that, if desired, Rabbit MQ communication can be TLS 
  enabled. When using `cfg~caf~worker~RabbitConfiguration`, the rabbit protocol used by services can be configured using 
  the CAF_RABBITMQ_PROTOCOL environment variable. The default value is 'amqp'.
  Consumers using the util-rabbitmq module will need to handle additional exceptions when creating a Rabbit connection
  through `RabbitUtil.java`. RabbitUtil.createRabbitConnection now requires a protocol argument.

