!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- **US857114:** Introduced `CAF_RABBITMQ_PROTOCOL` environment variable so that RabbitMQ URL protocol is customisable.
        This allows for TLS-enabled connections to be made to RabbitMQ if desired.
        By default, this variable is set to "amqp" so there is no change in behaviour unless specified. 

#### Known Issues

#### Breaking Changes
- **US749035**: Classic queues and priority queues are deprecated. 
When using `cfg~caf~worker~RabbitConfiguration.js`, the type of queue created by workers can be controlled by the ENV 
CAF_RABBITMQ_QUEUE_TYPE, this currently defaults to 'classic', but will change to 'quorum' in a future release.
