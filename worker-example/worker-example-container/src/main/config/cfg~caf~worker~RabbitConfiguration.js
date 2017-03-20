({
    backoffInterval: 1,
    maxBackoffInterval: 30,
    maxAttempts: 20,
    rabbitHost: getenv("CAF_RABBITMQ_HOST") || "rabbitmq",
    rabbitPort: getenv("CAF_RABBITMQ_PORT") || 5672,
    rabbitUser: getenv("CAF_RABBITMQ_USERNAME") || "guest",
    rabbitPassword: getenv("CAF_RABBITMQ_PASSWORD") || "guest"
});
