#### Version Number
${version-number}

#### New Features
- US969005: Add support for getting secrets from configurable sources.     
  - The RabbitMQ password can be retrieved from:
    - Environment variable named `CAF_RABBITMQ_PASSWORD` (enabled by default via `CAF_ENABLE_ENV_SECRETS`, defaults to `true`)
    - File content in path specified by environment variable named `CAF_RABBITMQ_PASSWORD_FILE` (enabled via `CAF_ENABLE_FILE_SECRETS`, defaults to `false`)

#### Known Issues
- None
