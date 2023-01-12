# worker-core

 This subproject contains the asynchronous microservice worker applcation
 code and various module implementations that have been designed to work
 with it.
 
 
## Definition of an asynchronous microservice worker

 The application `worker-core` has been written to meet specific requirements.
 While most workers on a software-as-a-service platform will likely be a good
 fit, it is worthwhile taking them into consideration to understand the choices
 made and how it may affect decisions when writing new components for it.
 
 - A worker receives a task as a single message from a queue
 - Each worker may handle multiple tasks at once
 - The input queue is set at start-up time
 - For each single task completed, a worker must emit one message
 - A worker can output a result message, or another task message
 - The workers are stateless, and so can be trivially scaled
 - A worker will not lose requests, and gracefully handle failure
 - A worker will provide metrics and healthchecks
 - Input and output messages are wrapped in a fixed container
 - A worker will not accept any new work if a shutdown is triggered
 - A worker will abort its current work if interrupted

 One of the key aspects of a microservice is that it can be scaled up or down
 to meet demand. This is easiest to do when the worker is stateless. In
 essence, this means a worker should not be aware (or need to be aware) of any
 other worker or its surrounding system. This way additional "clones" of it
 can be created or destroyed at will to provide scaling without affecting other
 workers. Because they may be destroyed, workers should not create any local
 data with the expectation it will be kept or maintained.
 
 
## The worker-core application
 
 This application is effectively managing the flow of data through the various
 components with some logic checking. To achieve this, it attempts to detect
 and initialise several modules at runtime which should be present on the
 classpath. Implementations that *require* to be packaged with `worker-core`
 are:
 
 - DataStore
 - WorkerFactory
 - ConfigurationSource
 - WorkerQueue
 - Codec
    
 The following components may *optionally* be packaged. If they are not found,
 a default no-op implementation will be used:
 
 - Cipher
    
 Once all the modules are found and instantiated, `worker-core` sets up a
 callback to register a new task, and passes this to the WorkerQueue along
 with the input and output queue names. The `WorkerQueue` implementation is
 then expected to handle incoming messages and refer them to the supplied
 callback, at which point the `WorkerFactory` will be required to provide a
 new instances of a `Worker` to handle the message. The `Worker` itself is
 wrapped in a `WorkerWrapper` and executed in a thread pool. Upon completion,
 be it success or failure, the application will then route the result message
 back to the `WorkerQueue` for publishing. Messages that cannot be passed to
 a `Worker` are rejected back to the `WorkerQueue`. Each application itself
 has a backlog of messages with an upper bound. The number of threads (in other
 words, the number of simultaneous tasks to perform) is dictated by the
 `WorkerFactory` supplied to the application.
 
 The `worker-core` application exposes health checks and metrics from itself
 and dependent modules to the Dropwizard admin port (default 8081).
 
 At time of writing, `worker-core` has no operations available on the REST
 port (default 8080).
 
### Configuration

 The following environment variables can optionally be set:
 - caf.appname: manual service name to be set if not in a Marathon environment
 - CAF_WORKER_ENABLE_DIVERTED_TASK_CHECKING: A boolean that indicates if the `to` field of a TaskMessage should be 
compared to the input current of the worker, redirecting the message to the queue in the `to` field if it does not match 
the current input queue. Default is True.

### Starting the application

 The following command-line should start the application:
 
 ```
 java -cp "*" com.hpe.caf.worker.core.WorkerApplication server [settings.yaml]
 ```
    
### The DataStore component

 The following classes and interfaces are relevant to the `DataStore` component:
 
 - DataStore: abstract class that acts as a base implementation.
 - DataStoreException: or any DataStore specific errors.
 - DataStoreMetricsReporter: interface for reporting back metrics to the core.
 - DataStoreProvider: interface for acquiring a DataStore.
    
 A `DataStore` itself is used to the archiving and retrieval of arbitrary data.
 This data may be required by a `Worker` to perform its task, or the `Worker`
 may wish to store a result directly to a storage component if the data is too
 large to fit within a queue. It is not used interally by `worker-core`.
 
### The WorkerQueue component

 The following classes and interfaces are relevant to the `WorkerQueue`
 component:
 
 - QueueException: for any WorkerQueue specific errors.
 - WorkerQueue : abstract class that acts as a base implementation.
 - WorkerQueueMetricsReporter: interface for reporting metrics back to the
  core.
 - WorkerQueueProvider: interface for acquiring a WorkerQueue.
 - TaskCallback: interface for representing a callback used by a
  WorkerQueue when it wishes to register the arrival of a new task with the
  application.
    
 A `WorkerQueue` is responsible for receiving messages from a queue and sending
 them to `worker-core`, and will receive results from `worker-core` to publish.
 It must also handle rejection of messages.
 
### The Worker and WorkerFactory components

 The following classes and interfaces are relevant to the `Worker` component:
 
 - Worker: abstract class that acts as a base implementation for something that
  does useful work, and returns a WorkerResponse.
 - WorkerFactory: abstract class that implementations are responsible for
  creating a worker given message data.
 - WorkerFactoryProvider: interface for acquiring a WorkerFactory.
 - TaskStatus: an enumeration that represents the possible states of a
  message.
 - TaskMessage: class that wraps all messages in and out of a worker.
  The task specific result data from the `Worker` will be contained within.
 - WorkerResponse: represents the requested action of a `Worker` after its
  work has been performed, typically indicating the core to return a result.
    
 A `WorkerFactory` creates instances of a `Worker` class that will perform
 actions upon message data it is given (which represents a task to do). The
 `WorkerFactory` is free to create the `Worker` however it wishes, but the
 `Worker` must be able to return a WorkerResponse which contains the
 serialised result data once it has completed. Implementations of `Worker`
 should use the utility methods `createSuccessResult(...)` or
 `createFailureResult(...)` to return results. Serialisation can be done
 using a `Codec` from the `WorkerFactory`, which is passed through from the
 application itself, or alternatively a `Worker` may use its own method of
 serialisation if it knows the inner message format. A `Worker` can also
 provide some "default" result data in case an unhandled exception is
 encountered.

 Depending on the application configuration, different versions of the Worker
 message API may be shared on the same queue during an upgrade. This means that
 a Worker may receive incoming tasks of verisons `n` and `n+1` during this
 period. If a Worker cannot tolerate this it should be documented. To make a
 Worker tolerant, one approach is to have multiple task and result classes in
 the package and the correct one (de)serialised depending upon the message API
 version. This can be done in the `WorkerFactory`. Alternatively, to use the
 same task/result class, field types must *never* be modified (but new fields
 can be added). A `Codec` can tolerate unknown fields by using
 `DecodeMethod.LENIENT` and passing this to the `deserialise` method. In
 practice however, if a queue shares message versions, it may make sense to
 allow newer Workers to accept older tasks with some necessary logic in the
 WorkerFactory, but for older Workers to reject newer tasks back onto the
 queue in the assumption that newer Workers will be deployed to handle these
 in a reasonable timeframe.

### Handling errors with worker implementations

 The general following rules should be adhered to by all Worker implementations:

 - For any explicit failures, return a failure result, not an exception
 - If the input message is not parsable, throw an InvalidTaskException
 - If the task cannot be accepted right now, throw a TaskRejectedException
 - If a transient error occurs in processing, throw a TaskRejectedException
 - If the worker receives an InterruptedException, propagate the InterruptedException
 - If a catastrophic error occurs in processing, throw TaskFailedException

 The `WorkerFactory` should identify whether the task message data is
 parsable and this is the first opportunity to throw an `InvalidTaskException`.
 Once a Worker is created with a task object, the framework will verify the
 object's constraints (if there are any), which is the second chance to throw
 `InvalidTaskException`. The constructor of the `Worker` can throw an
 `InvalidTaskException`. Finally a worker's `doWork()` method can thow an
 `InvalidTaskException`.

 While `InvalidTaskException` is a non-retryable case, there may be retryable
 scenarios for instance a brief disconnection from a temporary resources such as
 a database. If you have a health check in your `WorkerFactory` and this is
 currently failing, you may wish to throw `TaskRejectedException`, which will
 push the task back onto the queue. Once inside a Worker itself, either the
 code or the libraries used should be able to tolerate some amount of transient
 failures in connected resources, but if this still cannot be rectified in a
 reasonable time frame then it is also valid to throw `TaskRejectedException` from
 inside the Worker, with the understanding that any amount of work done so
 far will be abandoned.

 Throwing `TaskFailedException` should be a last resort, for situations that
 should not occur and are not recoverable. Most times, any checked exceptions
 that are caught and are a valid failure case should log the exception, (not
 propagate the exception). Workers derived from `AbstractWorker<>` should then
 call `createFailureResult(...)` perhaps using a sensible enumeration status code
 for your task.

 You do not need to handle the following, as the framework will handle it:

 - The input wrapper not being parsable
 - Connections to the queue dropping

#### Poison Messages

 A poison message is a message a worker is unable to handle. A message is
 deemed poisonous during processing when repeated catastrophic failure of the
 worker occurs. Regardless of how many times the message is retried, the worker
 will not be able to handle the message in a graceful manner.

 On receiving a message, a worker will attempt to process the message.  Should
 the worker crash during processing, the message will be returned to the
 `worker-input-queue` by the framework and a retry count append to the message
 headers.  The number of permitted retries is configurable within the
 `RabbitWorkerQueueConfiguration` file for a worker i.e. cfg_caf_dataprocessing_${worker}_RabbitWorkerQueueConfiguration.  
 A message will be retried until successful or until the retry count exceeds the
 permitted number of retries.  If the permitted number of retries is exceeded the
 message will be placed on the `worker-output-queue` by the framework, with a
 task status of “RESULT_EXCEPTION”.  
 
## Creating a container for a worker
 
 A container for a worker is generally made as a new subproject in Maven (or
 your preferred build system). It will have no code itself, but will have 
 dependencies upon `worker-core` and an implementation of all the required
 components, together with any start scripts. Fixed configuration parameters
 required at startup can be safely added as Java properties in the command
 line for starting the application. Configuration parameters that need to be
 variable should be left to be set by environment variables in your container
 deployment template. You will need the following dependencies in your project
 to create a fully functioning container:
 
 - worker-core
 - caf-api
 - An implementation of ConfigurationSource
 - An implementation of WorkerFactory
 - An implementation of DataStore
 - An implementation of WorkerQueue
 - An implementation of Codec
 - An implementation of Cipher (optional)
 
 
## Available metrics

 The following metrics are exposed on the Dropwizard admin port:
 
 - core.taskTimer: timer/histogram that gives timing data on the tasks it is
  performing.
 - core.backlogSize: the current number of tasks this worker instance has
  accepted but are not currently being worked upon (due to the threadpool
  being full).
 - core.uptime: the time in milliseconds since initialisation.
 - core.tasksReceived: the number of tasks registered by the WorkerQueue with
  the application.
 - core.taskRejected: the number of tasks rejected from the application. This
  typically implies no Worker could be instantiated to handle the message.
 - core.tasksSucceeded: the number of tasks that completed with a successful
  TaskResultStatus.
 - core.tasksFailed: the number of tasks that failed with an unsuccessful
  TaskResultStatus.
 - core.tasksAborted: the number of tasks that were aborted because they were
  indicated as requeued by the WorkerQueue
 - core.currentIdleTime: the time in milliseconds since the worker was doing
  anything useful.
 - core.inputSizes: histogram of input (task) message sizes in bytes
 - core.outputSize: histogram of output (result) messages sizes in bytes
 - config.lookups: the number of configuration lookups performed by the
  ConfigurationSource.
 - config.errors: the number of failures reported by the ConfigurationSource.
 - store.writes: the number of write requests to the DataStore.
 - store.reads: the number of read requests to the DataStore.
 - store.deletes: the number of delete requests to the DataStore.
 - store.errors: the number of errors encounted by the DataStore.
 - queue.received: the number of messages received by the WorkerQueue.
 - queue.published: the number of messages published by the WorkerQueue.
 - queue.rejected: the number of messages rejected by the WorkerQueue.
 - queue.dropped: the number of messages dropped by the WorkerQueue. This
  typically implies messages that were already rejected at least once and could
  still not be handled. Whether they were actually dropped or just routed
  elsewhere will depend upon the implementation.
 - queue.errors: the number of errors encountered by the WorkerQueue.


## Health checks within the worker framework

 All Dropwizard applications have support for health checks and the worker
 framework is no exception. The `caf-api` abstracts this away however so there
 is no need to know about Dropwizard or directly include its dependencies.

 The following components have health checks:

  - ConfigurationSource
  - DataStore
  - WorkerQueue
  - WorkerFactory

 Each of these classes implements the interface `HealthReporter` which enforces
 a method called `healthCheck()` which returns a `HealthResult`. Typically, a
 pre-made "healthy" result can be returned via `HealthResult.RESULT_HEALTHY`
 but it is possible to construct others. If the status in unhealthy, a
 message with additional information about the problem should be supplied.

 All of these health checks are exposed via the worker framework to the
 underlying Dropwizard health checks environment (which under the hood uses
 the Codahale Metrics library). Thus, the "/healthchecks" URL present on the
 Dropwizard admin port (default 8081) will output the result of all the
 health checks in JSON format. This includes the four health checks listed
 above along with in-built deadlock checks.

 If any of the health checks failed (returning unhealthy) then the HTTP
 response (along with the JSON) will have status 500, indicating an error. It
 is possible to utilise this with monitoring systems to look for this status
 code, so even parsing JSON is not required for basic monitoring.
 This kind of remote health check can be performed with Marathon by adding an
 appropriate section to your Marathon task descriptor. See the Marathon
 documentation for more information on how to do this. As a general
 reference, a single "unhealthy" return is unlikely to destroy the container
 in a reasonable Marathon descriptor. Typically, multiple consecutive
 failures are required to trigger destruction of the container instance, but
 this is entirely up to the developer. If you are producing a container that
 may be re-used by other applications, it may be worth investigating and
 advising in your documentation about the general behaviour of your health
 checks and how many consecutive failures would likely indicate beyond all
 doubt that the container is unhealthy.

 Note that a call to `healthCheck()` is expected to be trivial and take a
 negligible amount of time. As such, if a health check is of a much heavier
 nature, it is preferred to have a thread that periodically performs this and
 then the `healthCheck()` call merely returns the latest value available.

 Finally, health checks should not be relied upon to deal with startup error
 conditions. If a containerised application cannot initialise, it should
 fail-fast and terminate immediately, rather than wait for health checks to
 report unhealthy several times and eventually be forcibly terminated.


## Tutorial: creating a new Worker backend

 This will briefly go over the step-by-step process involved for creating a new
 `Worker` backend implementation. It is assumed you will be using supplied
 implementations of the other components (`WorkerQueue` etc).

 For this example implementation, we will use three subprojects. The first will
 be the actual code for the backend module itself. The second will be data that
 is shared between the consumer and producer, namely the Java classes that will
 represent the incoming task and the outgoing result. The thid will be a
 subproject to package the whole worker as a service to be deployed.

 In this trivial example, we will make a `Worker` that performs some text
 manipulation and also simulate some load by delaying the processing.

### The messages: input and output

 A good way to start thinking about a `Worker` is by what message it receives
 and produces. Since we are doing some text manipulation, we will need the
 text we want to manipulate, so this is an input. The only output we are
 generating is the manipulated text, so that is the sole member of the result
 message.

 It is important to understand the difference between what data should be in
 the incoming message and what should be in your worker configuration.
 Generally speaking, configuration should contain options that need to be
 modified that will change the operation of workers, but does not affect each
 individual worker between each run. In our example, the string we are going
 to manipulate is clearly task data, as each string may be different, but we
 will make the load simulation a configuration parameter so that each worker
 run will simulate the same sort of load.

 Input and output data represented by Java classes needs to be easily
 serialisable by implementations of Codec, so each member variable should have
 an appropriate getter and setter according to Java standards. For most Codecs,
 it will also need a no-argument constructor. Because of the serialisation,
 the input and output messages should not extend other classes, and to ensure
 this, the classes should also be declared final.

 Because these input and output messages define how to communicate with the
 Worker, should they change then the worker API version should be incremented.

 The input message will look a bit like this:

```
 package com.hpe.caf.test.worker.shared;


 public final class TestWorkerTask
 {
    private String taskString;
    private static final String WORKER_ID = "TestWorker";
    private static final int WORKER_API_VER = 1;


    public TestWorkerTask() { }


    public String getTaskString()
    {
        return taskString;
    }


    public void setTaskString(final String input)
    {
        this.taskString = input;
    }
 }
```

 Equivalently, our output message will be something like:

```
 package com.hpe.caf.test.worker.shared;


 public final class TestWorkerResult
 {
    private String resultString;


    public TestWorkerResult() { }


    public String getResultString()
    {
        return resultString;
    }


    public void setResultString(final String output)
    {
        this.resultString = output;
    }
 }
```

### Worker configuration

 With the messages complete, we should also make our configuration object for
 our worker. We only have one thing here, but again since the configuration
 objects that come from a `ConfigurationSource` need to be deserialised, they
 should have a no-argument constructor and getters and setters just like the
 message objects we created before. Here is our configuration class, note this
 is also final as they are typically serialised:

```
 package com.hpe.caf.test.worker;

 import com.hpe.caf.api.Configuration;

 import javax.validation.constraints.NotNull;
 import javax.validation.constraints.Size;


 @Configuration
 public final class TestWorkerConfiguration
 {
    private long sleepTime;
    @NotNull
    @Size(min = 1)
    private String resultQueue;
    @Min(1)
    @Max(50)
    private int threads;


    public TestWorkerConfiguration() { }


    public long getSleepTime()
    {
        return sleepTime;
    }


    public void setSleepTime(final long time)
    {
        this.sleepTime = Math.max(0, time);
    }


    public String getResultQueue()
    {
        return resultQueue;
    }


    public void setResultQueue(final String queue)
    {
        this.resultQueue = queue;
    }


    public int getThreads()
    {
        return threads;
    }


    public void setThread(final int threadCount)
    {
        this.threads = threadCount;
    }
 }
```

 It's probably worth creating an actual serialised configuration file now, as
 we'll need this to run our worker. This is actually a trivial case, and you
 could work it out yourself, or use the `util-tools` package. Let's say we're
 going to use JSON for configuration files in our application. If we grab the
 `util-tools` jar file, and put the `codec-json` jar with it along with our
 freshly compiled `test-worker-shared` jar, we can do this:

```
 java -cp "*" com.hpe.caf.util.GenerateConfig
    com.hpe.caf.test.worker.shared.TestWorkerConfiguration
```

### Creating the factory and provider

 The job of a `WorkerFactory` is to produce a `Worker` given some serailised
 task data which has originated from a `WorkerQueue` of some nature.
 Since we already have the task data passed in for each task via the
 `getWorker(...)` method, the only other things we need is our
 configured sleep time, and also something (a `Codec`) with which to
 serialise our result to return. For the purposes of this tutorial, the health
 check will always return successful, and the invalid task respones will be put
 onto the same result queue. So the `WorkerFactory` looks like this:

```
 package com.hpe.caf.test.worker;

 import com.hpe.caf.api.Codec;
 import com.hpe.caf.api.worker.WorkerException;
 import com.hpe.caf.api.worker.WorkerFactory;

 import java.util.Objects;


 public class TestWorkerFactory
  extends DefaultWorkerFactory<TestWorkerConfiguration, TestWorkerTask>
 {
    private final Codec codec;
    private final long sleepTime;
    private final String resultQueue;


    public TestWorkerFactory(final Codec codec, final long sleepTime,
                             final String resultQueue)
    {
      this.codec = Objects.requireNonNull(codec);
      this.sleepTime = sleepTime;
      this.resultQueue = Objects.requireNonNull(resultQueue);
    }


    @Override
    public Worker createWorker(TestWorkerTask task)
    {
      long sleep = getConfiguration().getSleepTime();
      String queue = getConfiguration().getResultQueue();
      return new TestWorker(task, getCodec(), sleep, queue);
    }


    @Override
    public int getWorkerThreads()
    {
      return getConfiguration().getThreads();
    }


    @Override
    public String getInvalidTaskQueue()
    {
      return resultQueue;
    }


    @Override
    public HealthResult healthCheck()
    {
      return HealthCheck.RESULT_HEALTY;
    }
 }
```

 We haven't made the TestWorker yet, but we already know what it needs. This
 helps us define how our `Worker` will function. But we need one more small
 class, which is effectively boilerplate, for `worker-core` itself to acquire
 our TestWorkerFactory - which is an implementation of `WorkerFactoryProvider`.
 A provider must have a no-argument constructor, so it can be instantiated by
 anything using `ComponentLoader`. The separation of `WorkerFactory` from a
 `WorkerFactoryProvider` allows us to keep constructor safety with our actual
 `WorkerFactory` while still allowing the plug-in nature of the components with
 the worker application itself. The provider must also give some basic data
 on identifying the worker. Finally it is worth mentioning that if your Worker
 depends upon any external resources, then it may be worth adding a health
 check here so that there is some way to monitor these underlying resources
 and prompt automated systems or ops teams to take action when necessary.

```
 package com.hpe.caf.test.worker;

 import com.hpe.caf.api.Codec;
 import com.hpe.caf.api.ConfigurationException;
 import com.hpe.caf.api.ConfigurationSource;
 import com.hpe.caf.api.worker.DataSource;
 import com.hpe.caf.api.worker.WorkerException;
 import com.hpe.caf.api.worker.WorkerFactoryProvider;

 import java.util.Objects;


 public class TestWorkerFactoryProvider implements WorkerFactoryProvider
 {
    @Override
    public WorkerFactory getWorkerFactory(final ConfigurationSource config,
        final DataStore store, final Codec codec) throws WorkerException
    {
        try {
            Objects.requireNonNull(config);
            TestWorkerConfiguration testConfig =
                config.getConfiguration(TestWorkerConfiguration.class);
            long sleepTime = config.getSleepTime();
            String resultQueue = config.getResultQueue();
            return new TestWorkerFactory(codec, testConfig,
                                         sleepTime, resultQueue);
        } catch ( ConfigurationException e ) {
            throw new WorkerException("Failed to create factory", e);
        }
    }
 }
```

 Note that our crude example worker isn't using a `DataStore` so we effectively
 throw it away here. We also follow good encapsulation logic and only pass in
 the data the TestWorkerFactory actually needs rather than the whole instance
 of TestWorkerConfiguration.

### Creating the Worker

 We actually already know what the implementation of `Worker` should mostly
 look like - we've defined what it does, what the input and output should be,
 and even what the constructor should look like. In addition, the `Worker` base
 class enforces us to provide some methods. In essence the main part is that
 the method `doWork()` will return a `WorkerResponse`. There are various
 utility methods to aid in creating these responses, as demonstrated here:

```
 package com.hpe.caf.test.worker;

 import com.hpe.caf.api.Codec;
 import com.hpe.caf.api.CodecException;
 import com.hpe.caf.api.worker.Worker;
 import com.hpe.caf.api.worker.WorkerException;
 import com.hpe.caf.test.worker.shared.TestWorkerTask;
 import com.hpe.caf.test.worker.shared.TestWorkerResult;

 import java.nio.charset.StandardCharsets;
 import java.util.Objects;


 public class TestWorker extends DefaultWorker<TestWorkerTask,TestWorkerResult>
 {
    private final long sleepTime;
    private final String input;


    public TestWorker(final TestWorkerTask task, final Codec codec,
                      final long sleepTime, final String resultQueue)
        throws WorkerException
    {
        super(task, resultQueue, codec);
        this.sleepTime = sleepTime;
        this.input = Objects.requireNonNull(getTask().getTaskString());
    }


    /**
     * Transform the input String from the task data to upper case and sleep
     * for a while.
    **/
    @Override
    public WorkerResponse doWork()
        throws InterruptedException
    {
        String output = input.toUpperCase();
        Thread.sleep(sleepTime);
        return createSuccessResult(createResultObject(output));
    }


    @Override
    public String getWorkerIdentifier()
    {
        return TestWorkerTask.WORKER_ID;
    }


    @Override
    public int getWorkerApiVersion()
    {
        return TestWorkerTask.WORKER_API_VER;
    }


    private TestWorkerResult createResultObject(final String str)
    {
        TestWorkerResult res = new TestWorkerResult();
        res.setResultString(str);
        return res;
    }
 }
```

 Our API verison is obviously 1, but if we changed the format of the
 input or output messages that we handle, we should increase this number.

 The main point here is that we create everything we possibly can to ensure
 sensible operation of our `Worker` in the constructor.

 All Workers should be mindful for the Thread interrupt flag. If the Worker
 receives an `InterruptedException` it should propagate this. Periodically,
 particularly in long-lived loops, Worker implementations should also check
 the state of the current Thread's intterupt flag via the use of
 `checkIfInterrupted()`, which will throw the necessary exception if required.
 Failure to do this may lead to duplicate messages and results being return
 from the Worker in the case of non-standard events such as queue
 connection abnormalities.

### Advertising your new Worker to the core microservice application

 Now you've made everything, you need to announce it. Just putting your new
 TestWorker and factories on the classpath isn't sufficient. You'll need to
 make a services identifier that the Java ServiceLoader will pick up on.
 How you made an entry in `META-INF/services` may vary upon your build
 system, but in Maven, you need to make a `src/main/resources` directory,
 and then make a `META-INF` directory underneath that. The file you need to
 make will have the fully qualified class name of the interface (or base
 class) that the Java ServiceLoader will be looking for, in this case it will
 be `WorkerFactoryProvider`, and the file contents will be a single line which
 consists of the fully qualified class name of your implementation. So in this
 case, the file will be called
 `META-INF/services/com.hpe.caf.api.worker.WorkerFactoryProvider` and will
 have the line `com.hpe.caf.test.worker.TestWorkerFactoryProvider`.

### Putting it all together

 Now we have our complete implementation, all we have to do is package it
 all together. You may just be packaging a complete jar, or perhaps a tar
 file using a Maven assembly, or a Docker container. Either way, you will want
 to include the following dependencies in your container subproject and
 make sure it is all packaged together:

 - The test worker code subproject
 - The test worker shared objects subproject (task and result message classes)
 - The `worker-core` application itself
 - A `WorkerQueue` module
 - A `DataStore` module - even though we are not using it here
 - A `ConfigurationSource` module

 You will also need to put the serialised configuration somewhere sensible
 which will depend upon your `ConfigurationSource`. Finally, don't forget to
 set the necessary environment variables for your worker container. You'll need
 an application name, input queue, and output queue. See the documentation on
 `worker-core` for more details.
