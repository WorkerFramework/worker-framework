# worker-queue-rabbit

 This is an implementation of a `WorkerQueue` that uses RabbitMQ with the Lyra
 client library.


## Configuration

 The configuration source for this module is `RabbitWorkerQueueConfiguration`.
 The following configuration options are present:

- prefetchBuffer: the number of additional messages (tasks) to request from the RabbitMQ server beyond the number of tasks the worker can simultaneously handle. Minimum 0, Maximum 100000.
- inputQueue: the routing key for a direct exchange (ie. queue name) to receive input tasks from, this must be set
- retryQueue: the routing key to use for sending messages to retry to, this may be the same as the inputQueue, and will default to this if unset application, and messages that exceed the retryLimit, this must be set
- retryLimit: the maximum number of retries before sending the messages to the rejectedQueue, must be at least 1

 Note this module expects a valid `RabbitConfiguration` file to be present.
 See the `worker-configs` module for more details on this.


## Usage

 A RabbitMQ server must be available with appropriate credentials. The code
 uses two channels, one for the incoming queue and one for the outgoing, with
 each handled on a separate internal thread. This should be appropriate as
 long as the time spent performing tasks is significantly greater than the
 time taken to process messages.

 As this implementation uses the Lyra client, all connection failures will be
 retried, and dropped connections will be re-established, up to the maximum
 number of attempts specified in the configuration. Health checks will report
 as failed if the RabbitMQ connection is down. If the connection re-establishes
 successfully, the module will notify `worker-core` to abort current tasks to
 avoid duplication as much as possible, as the tasks will have already been
 requeued by the RabbitMQ server. Note this should make the window for possible
 duplicate responses small, but not impossible.

 Typically the prefetchBuffer should be 0, unless you have very short tasks
 (that don't take a long time to process) and you wish to reduce the amount
 of I/O chatter between workers and the RabbitMQ host.

 Consumed messages will only be acknowledged once the result has been published
 to the output queue, and the published response was confirmed by the server.

 Messages that the `worker-core` application deems as invalid (i.e. unparseable)
 will be placed on to the worker output queue with an associated error response.

 Messages the module encounters that are marked 'redelivered' by RabbitMQ are
 republished to the 'retry' queue with a retry count. This retry queue can be
 the same as the normal input queue, but this has some limitations. If these
 two queues are the same, then retried messages will always go to the back of
 the input queue, so if you desire to maintain some semblance of order you
 should have a separate retry queue and a Worker instance listening on this
 retry queue. The other scenario is when your Worker is running unmanaged
 code that can segfault/crash Java. In this case, all running tasks will be
 retried and potentially valid tasks can then end up being in the output queue, with an associated error response.
 To avoid this, the simplest way is to have single-threaded Worker instances
 for unmanaged code listening on the retry queue and use the autoscaler to
 scale as necessary. The alternative is to have only the Worker instances on
 the retry queue to be single threaded.

 Messages that are marked 'redelivered' and already have a retry count stamp
 that exceeds the retry limit will be put on the output queue with associated error response.

 Messages that cause a `TaskRejectedException` at task registration time will
 be republished back onto the input queue, but do not count towards the retry
 limit.

 ### Header stamping

 The module uses the following headers that may be stamped on messages:  
 - `x-caf-worker-retry`: a numerical count of the number of retries
  attempted for this message, only present for retried messages
 - `x-caf-worker-retry-lmit`: a numerical representation of the number of retries allowed before a message will be deemed poisoned and moved to the worker's output queue  
 - `x-caf-worker-rejected`: present for all messages published to the
  rejected queue, possible values are `TASKMESSAGE_INVALID` and
  `RETRIES_EXCEEDED`   


## Failure modes

 The following scenarios will prevent the module from initialising:
 
- The configuration file is invalid
- A connection to the RabbitMQ server cannot be established
- A queue of a differing type has already been declared with the routing key

 The following scenarios have been identified as possible runtime failure modes
 for this module:

- Non-transient RabbitMQ server failures
- The prefetchBuffer exceeding the maximum backlog of the worker application


## Maintainers

 The following people are contacts for developing and maintaining this module:

 - Richard Hickman (Cambridge, UK, richard.hickman@microfocus.com)
