({
    prefetchBuffer: 0,
    inputQueue: getenv("CAF_WORKER_INPUT_QUEUE") || "example-input-1",
    retryQueue: getenv("CAF_WORKER_RETRY_QUEUE") || getenv("CAF_WORKER_INPUT_QUEUE") || "example-input-1",
    rejectedQueue: getenv("CAF_WORKER_REJECTED_QUEUE") || "test-rejected-1",
    retryLimit: getenv("CAF_WORKER_RETRY_LIMIT") || 2
});
