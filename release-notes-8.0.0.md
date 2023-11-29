!not-ready-for-release!

#### Version Number
${version-number}

#### New Features

#### Known Issues

#### Breaking Changes
- **US749035**: Support for the creation of priority queues has been removed and classic queues deprecated. 
When using `cfg~caf~worker~RabbitConfiguration.js`, the type of queue created by workers can be controlled by the ENV 
CAF_RABBITMQ_QUEUE_TYPE, this currently defaults to 'classic', but will change to 'quorum' in a future release.
