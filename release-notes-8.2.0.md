!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- US969005: Add support for getting secrets from various sources   
  - The RabbitMQ password will be retrieved from various sources in a prescribed order of precedence:
    - Environment variable named `CAF_RABBITMQ_PASSWORD`
    - File content in path specified by environment variable named `CAF_RABBITMQ_PASSWORD_FILE`
    - System property named `CAF.CAF_RABBITMQ_PASSWORD`

#### Known Issues
