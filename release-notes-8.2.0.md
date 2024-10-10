!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- US969005: Add support for getting secrets from configurable sources.     
  - The RabbitMQ password can be retrieved from:
    - Environment variable named `CAF_RABBITMQ_PASSWORD` (enabled by default via `CAF_GET_SECRETS_FROM_ENV`, defaults to `true`)
    - File content in path specified by environment variable named `CAF_RABBITMQ_PASSWORD_FILE` (enabled via `CAF_GET_SECRETS_FROM_FILE`, defaults to `false`)

#### Known Issues
