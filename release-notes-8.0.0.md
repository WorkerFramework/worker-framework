!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- **US857114**: Introduced `CAF_RABBITMQ_PROTOCOL` environment variable so that RabbitMQ URL protocol is customisable.
        This allows for TLS-enabled connections to be made to RabbitMQ if desired.
        By default, this variable is set to "amqp" so there is no change in behaviour unless specified. 
- **US857114:** Quorum queues leveraging 'x-delivery-count' for the handling of poison messages.

#### Bug Fixes
- **I445035**: Workers now attempt to complete all in-progress and pre-fetched tasks before shutting down.
- **I874162**: Fixed issue where the `JobStatusResponseCache` was not working for HTTPS requests.

#### Known Issues

#### Breaking Changes
- **US749035**: Classic queues and priority queues are deprecated, priority queues can still be created however it is no
  longer possible to publish messages with a priority.
  When using `cfg~caf~worker~RabbitConfiguration.js`, the type of queue created by workers can be controlled by the ENV
  CAF_RABBITMQ_QUEUE_TYPE, this currently defaults to 'quorum', but will be removed in a future release.
