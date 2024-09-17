# worker-queue-sqs

This is an implementation of a `WorkerQueue` that uses SQS with the AWS SQS java client library.


## Configuration

The configuration source for this module is `SQSWorkerQueueConfiguration`.
The following configuration options are present:

- maxNumberOfMessages: the number of additional messages (tasks) to request from the SQS server beyond the number of tasks the worker can simultaneously handle. Minimum 0, Maximum 10.
- longPollInterval: the duration (in seconds) for which the call waits for a message to arrive in the queue before returning.
- visibilityTimeout: a visibility timeout, a period of time during which SQS prevents all consumers from receiving and processing the same message.
- messageRetentionPeriod: the length of time, in seconds, for which SQS retains a message. Minimum 60 (one minute), Maximum 1,209,600 (14 days).
- maxDeliveries: the number of times a message will be delivered before being moved to the dead-letter queue.
- maxInflightMessages: the time that a worker will wait before publishing messages queued for downstream. Minimum 1, Maximum 120,000
- publisherWaitTimeout: the number of inflight messages that a worker can handle for a particular queue. Minimum 0 (no wait), Maximum 300 (5 minutes).
- inputQueue: the queue to retrieve messages from.
- pausedQueue: the queue to use to send messages to when a job is paused, this is optional, and if not set, messages sent to a worker when a job is paused will be processed as normal (as if the job was not paused).
- retryQueue: the queue to use for sending messages to retry to, this may be the same as the inputQueue, and will default to this if unset application.
- retryLimit: the maximum number of retries before sending the messages to the rejectedQueue, must be at least 1

Note this module expects a valid `SQSConfiguration` file to be present.
See the `worker-configs` module for more details on this.


## Usage

A SQS server must be available with appropriate credentials.

Consumed messages will only be acknowledged/deleted once the result has been published
to the output queue, and the published response was confirmed by the server.

Messages that the `worker-core` application deems as invalid (i.e. unparseable)
will be placed on to the worker output queue with an associated error response.

Messages the module encounters on the dead letter queue will have been delivered up to the `maxDeliveries` limit, 
will be put on the output queue with associated error response.

Messages that cause a `TaskRejectedException` at task registration time will
be republished back onto the input queue.

### Header stamping

The module uses the following headers that may be stamped on messages:
- `x-caf-worker-rejected`: present for all messages published to the
  rejected queue, possible values are `TASKMESSAGE_INVALID` and
  `RETRIES_EXCEEDED`


## Failure modes

The following scenarios will prevent the module from initialising:

- The configuration file is invalid
- A connection to the SQS server cannot be established
- A queue with different attributes has already been declared with the same name.

The following scenarios have been identified as possible runtime failure modes
for this module:
