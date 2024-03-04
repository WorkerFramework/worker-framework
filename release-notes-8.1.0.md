!not-ready-for-release!

#### Version Number
${version-number}

#### New Features

#### Known Issues

#### Breaking Changes
- **US887031:** A secure Trust Manager is now being initialized during the TLS connection/session negotiation to ensure only allowed certificates are used. Consumers using the util-rabbitmq module will need to handle additional exceptions when creating a Rabbit connection using `RabbitUtil.java`.
