# worker-queue-rabbit

---

 This is an implementation of a `WorkerQueue` that uses RabbitMQ with the Lyra
 client library.


## Configuration

 The configuration source for this module is `RabbitWorkerQueueConfiguration`.
 The following configuration options are present:

 - rabbitHost: the hostname of the RabbitMQ server. Must not be null and must
  not be empty.
 - rabbitUser: username for authentication with the RabbitMQ server. Must not
  be null and must not be empty.
 - rabbitPassword: password for authentication with the RabbitMQ server. This
  field is encrypted. Must not be null and must not be empty.
 - maxAttempts: integer specifying maximum number of attempts when performing
  a request to the RabbitMQ server. Minimum 0, maximum 1000.
 - backoffInterval: integer specifying base number of seconds between a
  backoff/retry. This will double each attempt up to the maximum. Minimum 1,
  maximum 1000.
 - maxBackoffInterval: integer specifying the maximum number of seconds between
  backoff/retry attempts. Minimum 1, maximum 1000.
 - deadLetterExchange: name of the RabbitMQ exchange where messages that cannot
  be handled at all are routed to. Must not be null and must not be empty.
 - prefetchBuffer: the number of additional messages (tasks) to request from
  the RabbitMQ server beyond the number of tasks the worker can simultaneously
  handle. Minimum 0, maximum 1000.


## Usage

 A RabbitMQ server must be available with appropriate credentials. The code
 uses two channels, one for the incoming queue and one for the outgoing, with
 each handled on a separate internal thread. This should be appropriate as
 long as the time spent performing tasks is significantly greater than the
 time taken to process messages.

 As this implementation uses the Lyra client, all connection failures will be
 retried, and dropped connections will be re-established, up to the maximum
 number of attempts specified in the configuration. Health checks will report
 as failed if the RabbitMQ connection is down.

 Typically the prefetchBuffer should be 0, unless you have very short tasks
 (that don't take a long time to process) and you wish to reduce the amount
 of I/O chatter between workers and the RabbitMQ host.

 Messages that get rejected will be put back on the queue. A subsequent
 failure to begin work on a message that already failed will be put into the
 dead letter exchange.

 Consumed messages will only be acknowledged once the result has been published
 to the output queue.


## Failure modes

 The following scenarios will prevent the module from initialising:

 - The configuration file is invalid
 - A connection to the RabbitMQ server cannot be established
 - A queue of a differing type has already been declared with the same name

 The following scenarios have been identified as possible runtime failure modes
 for this module:

 - Non-transient RabbitMQ server failures
 - The prefetchBuffer exceeding the maximum backlog of the worker application


## Maintainers

 The following people are contacts for developing and maintaining this module:

 - Richard Hickman (Cambridge, UK, richard.hickman@hp.com)
