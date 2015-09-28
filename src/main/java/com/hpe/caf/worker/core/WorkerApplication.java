package com.hpe.caf.worker.core;


import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.hpe.caf.api.BootstrapConfiguration;
import com.hpe.caf.api.Cipher;
import com.hpe.caf.api.CipherException;
import com.hpe.caf.api.CipherProvider;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.ConfigurationSourceProvider;
import com.hpe.caf.api.ServicePath;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.DataStoreProvider;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerFactoryProvider;
import com.hpe.caf.api.worker.WorkerQueue;
import com.hpe.caf.api.worker.WorkerQueueProvider;
import com.hpe.caf.cipher.NullCipherProvider;
import com.hpe.caf.config.system.SystemBootstrapConfiguration;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * This is the main HP SaaS asynchronous micro-service worker entry point.
 * On startup, it will identify implementations of necessary components (such as a queue,
 * data store, and backend worker), and create a WorkerCore object which will handle
 * the logic of data flow between these components. The application class itself here
 * just wrappers this and exposes health checks and metrics.
 */
public final class WorkerApplication extends Application<WorkerConfiguration>
{
    private final long startTime = System.currentTimeMillis();
    private static final Logger LOG = LoggerFactory.getLogger(WorkerApplication.class);


    /**
     * Entry point for the asynchronous micro-service worker framework.
     * @param args command-line args, currently not used by the worker
     * @throws Exception if the worker cannot startup
     */
    public static void main(final String[] args)
        throws Exception
    {
        new WorkerApplication().run(args);
    }


    /**
     * Get the default thread pool executor used by the worker framework.
     * @param nThreads the number of threads to initialise the new thread pool with
     * @return an instance of the default thread pool executor used by the worker framework
     */
    public static ThreadPoolExecutor getDefaultThreadPoolExecutor(final int nThreads)
    {
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }


    /**
     * Package private. If you wish to test a worker framework, try using WorkerCore.
     */
    WorkerApplication() { }


    /**
     * Start the asynchronous worker micro-service.
     * @param workerConfiguration the internal worker service internal configuration
     * @param environment the dropwizard environment, for setting up metrics and REST configuration
     */
    @Override
    public void run(final WorkerConfiguration workerConfiguration, final Environment environment)
        throws QueueException, ModuleLoaderException, CipherException, ConfigurationException, DataStoreException, WorkerException
    {
        LOG.debug("Starting up");
        BootstrapConfiguration bootstrap = new SystemBootstrapConfiguration();
        Cipher cipher = ModuleLoader.getService(CipherProvider.class, NullCipherProvider.class).getCipher(bootstrap);
        ServicePath path = bootstrap.getServicePath();
        Codec codec = ModuleLoader.getService(Codec.class);
        ConfigurationSource config = ModuleLoader.getService(ConfigurationSourceProvider.class).getConfigurationSource(bootstrap, cipher, path, codec);
        WorkerFactoryProvider workerProvider = ModuleLoader.getService(WorkerFactoryProvider.class);
        WorkerQueueProvider queueProvider = ModuleLoader.getService(WorkerQueueProvider.class);
        DataStore store = ModuleLoader.getService(DataStoreProvider.class).getDataStore(config);
        final int nThreads = Math.max(1, workerProvider.getWorkerThreads());
        ThreadPoolExecutor tpe = getDefaultThreadPoolExecutor(nThreads);
        WorkerQueue workerQueue = queueProvider.getWorkerQueue(config, nThreads);
        WorkerFactory workerFactory = workerProvider.getWorkerFactory(config, store, codec);
        WorkerCore core = new WorkerCore(codec, tpe, workerQueue, workerFactory, path);
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                core.shutdown();
                store.shutdown();
                config.shutdown();
            }
        });
        initCoreMetrics(environment.metrics(), core);
        initComponentMetrics(environment.metrics(), config, store, core);
        environment.healthChecks().register("queue", new WorkerHealthCheck(core.getWorkerQueue()));
        environment.healthChecks().register("configuration", new WorkerHealthCheck(config));
        environment.healthChecks().register("store", new WorkerHealthCheck(store));
        environment.healthChecks().register("worker", new WorkerHealthCheck(workerFactory));
        core.start();
    }


    private void initCoreMetrics(final MetricRegistry metrics, final WorkerCore core)
    {
        metrics.register(MetricRegistry.name("core.taskTimer"), WorkerWrapper.getTimer());
        metrics.register(MetricRegistry.name("core.backlogSize"), (Gauge<Integer>) core::getBacklogSize);
        metrics.register(MetricRegistry.name("core.uptime"), (Gauge<Long>) () -> System.currentTimeMillis() - startTime);
        metrics.register(MetricRegistry.name("core.tasksReceived"), (Gauge<Long>) core.getStats()::getTasksReceived);
        metrics.register(MetricRegistry.name("core.tasksRejected"), (Gauge<Long>) core.getStats()::getTasksRejected);
        metrics.register(MetricRegistry.name("core.tasksSucceeded"), (Gauge<Long>) core.getStats()::getTasksSucceeded);
        metrics.register(MetricRegistry.name("core.tasksFailed"), (Gauge<Long>) core.getStats()::getTasksFailed);
        metrics.register(MetricRegistry.name("core.tasksAborted"), (Gauge<Long>) core.getStats()::getTasksAborted);
        metrics.register(MetricRegistry.name("core.currentIdleTime"), (Gauge<Long>) core::getCurrentIdleTime);
    }


    private void initComponentMetrics(final MetricRegistry metrics, final ConfigurationSource config, final DataStore store, final WorkerCore core)
    {
        metrics.register(MetricRegistry.name("config.lookups"), (Gauge<Integer>) config::getConfigurationRequests);
        metrics.register(MetricRegistry.name("config.errors"), (Gauge<Integer>) config::getConfigurationErrors);
        if ( store.getMetrics() != null ) {
            metrics.register(MetricRegistry.name("store.writes"), (Gauge<Integer>) store.getMetrics()::getStoreRequests);
            metrics.register(MetricRegistry.name("store.reads"), (Gauge<Integer>) store.getMetrics()::getRetrieveRequests);
            metrics.register(MetricRegistry.name("store.errors"), (Gauge<Integer>) store.getMetrics()::getErrors);
        }
        if ( core.getWorkerQueue().getMetrics() != null ) {
            metrics.register(MetricRegistry.name("queue.receieved"), (Gauge<Integer>) core.getWorkerQueue().getMetrics()::getMessagesReceived);
            metrics.register(MetricRegistry.name("queue.published"), (Gauge<Integer>) core.getWorkerQueue().getMetrics()::getMessagesPublished);
            metrics.register(MetricRegistry.name("queue.rejected"), (Gauge<Integer>) core.getWorkerQueue().getMetrics()::getMessagesRejected);
            metrics.register(MetricRegistry.name("queue.dropped"), (Gauge<Integer>) core.getWorkerQueue().getMetrics()::getMessagesDropped);
            metrics.register(MetricRegistry.name("queue.errors"), (Gauge<Integer>) core.getWorkerQueue().getMetrics()::getQueueErrors);
        }
    }
}
