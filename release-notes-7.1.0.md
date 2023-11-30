!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- **US857114:** Introduced `CAF_RABBITMQ_PROTOCOL` environment variable so that RabbitMQ URL protocol is customisable.
        This allows for TLS-enabled connections to be made to RabbitMQ if desired.
        By default, this variable is set to "amqp" so there is no change in behaviour unless specified. 

#### Known Issues
