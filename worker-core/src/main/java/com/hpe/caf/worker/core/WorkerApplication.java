/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.worker.core;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.hpe.caf.api.BootstrapConfiguration;
import com.hpe.caf.api.CafConfigurationDecoderProvider;
import com.hpe.caf.api.Cipher;
import com.hpe.caf.api.CipherException;
import com.hpe.caf.api.CipherProvider;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationDecoderProvider;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSourceProvider;
import com.hpe.caf.api.Decoder;
import com.hpe.caf.api.ManagedConfigurationSource;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.DataStoreProvider;
import com.hpe.caf.api.worker.ManagedDataStore;
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.MessagePriorityManager;
import com.hpe.caf.api.worker.MessagePriorityManagerProvider;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerFactoryProvider;
import com.hpe.caf.api.worker.WorkerQueueProvider;
import com.hpe.caf.cipher.NullCipherProvider;
import com.hpe.caf.config.system.SystemBootstrapConfiguration;
import com.hpe.caf.naming.ServicePath;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;
import com.hpe.caf.util.jerseycompat.Jersey2ServiceIteratorProvider;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.internal.ServiceFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ResponseCache;
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
        ServiceFinder.setIteratorProvider(new Jersey2ServiceIteratorProvider());
        new WorkerApplication().run(args);
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
        
        ResponseCache.setDefault(new JobStatusResponseCache());
        BootstrapConfiguration bootstrap = new SystemBootstrapConfiguration();
        Cipher cipher = ModuleLoader.getService(CipherProvider.class, NullCipherProvider.class).getCipher(bootstrap);
        ServicePath path = bootstrap.getServicePath();
        Codec codec = ModuleLoader.getService(Codec.class);
        ConfigurationDecoderProvider decoderProvider = ModuleLoader.getService(ConfigurationDecoderProvider.class,
                                                                               CafConfigurationDecoderProvider.class);
        Decoder decoder = decoderProvider.getDecoder(bootstrap, codec);
        ManagedConfigurationSource config = ModuleLoader.getService(ConfigurationSourceProvider.class).getConfigurationSource(bootstrap, cipher, path, decoder);
        WorkerFactoryProvider workerProvider = ModuleLoader.getService(WorkerFactoryProvider.class);
        WorkerQueueProvider queueProvider = ModuleLoader.getService(WorkerQueueProvider.class);
        ManagedDataStore store = ModuleLoader.getService(DataStoreProvider.class).getDataStore(config);
        WorkerFactory workerFactory = workerProvider.getWorkerFactory(config, store, codec);
        WorkerThreadPool wtp = WorkerThreadPool.create(workerFactory);
        final int nThreads = workerFactory.getWorkerThreads();
        ManagedWorkerQueue workerQueue = queueProvider.getWorkerQueue(config, nThreads);
        MessagePriorityManagerProvider priorityManagerProvider = ModuleLoader.getService(MessagePriorityManagerProvider.class);
        MessagePriorityManager priorityManager = priorityManagerProvider.getMessagePriorityManager(config);

        WorkerCore core = new WorkerCore(codec, wtp, workerQueue, priorityManager, workerFactory, path);
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                LOG.debug("Shutting down");
                workerQueue.shutdownIncoming();
                wtp.shutdown();
                try {
                    wtp.awaitTermination(10_000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    LOG.warn("Shutdown interrupted", e);
                    Thread.currentThread().interrupt();
                }
                workerQueue.shutdown();
                workerFactory.shutdown();
                store.shutdown();
                config.shutdown();
            }
        });
        initCoreMetrics(environment.metrics(), core);
        initComponentMetrics(environment.metrics(), config, store, core);
        
        GatedHealthProvider gatedHealthProvider = new GatedHealthProvider(workerQueue);
        environment.healthChecks().register("queue", new GatedHealthCheck("queue", gatedHealthProvider, new WorkerHealthCheck(core.getWorkerQueue())));
        environment.healthChecks().register("configuration", new GatedHealthCheck("configuration", gatedHealthProvider, new WorkerHealthCheck(config)));
        environment.healthChecks().register("store", new GatedHealthCheck("store", gatedHealthProvider, new WorkerHealthCheck(store)));
        environment.healthChecks().register("worker", new GatedHealthCheck("worker", gatedHealthProvider, new WorkerHealthCheck(workerFactory)));
        
        core.start();
    }

    @Override
    public void initialize(Bootstrap<WorkerConfiguration> bootstrap)
    {
        bootstrap.setConfigurationSourceProvider(
            new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false, true))
        );
    }

    private void initCoreMetrics(final MetricRegistry metrics, final WorkerCore core)
    {
        metrics.register(MetricRegistry.name("core.taskTimer"), StreamingWorkerWrapper.getTimer());
        metrics.register(MetricRegistry.name("core.backlogSize"), (Gauge<Integer>) core::getBacklogSize);
        metrics.register(MetricRegistry.name("core.uptime"), (Gauge<Long>) () -> System.currentTimeMillis() - startTime);
        metrics.register(MetricRegistry.name("core.tasksReceived"), (Gauge<Long>) core.getStats()::getTasksReceived);
        metrics.register(MetricRegistry.name("core.tasksRejected"), (Gauge<Long>) core.getStats()::getTasksRejected);
        metrics.register(MetricRegistry.name("core.tasksSucceeded"), (Gauge<Long>) core.getStats()::getTasksSucceeded);
        metrics.register(MetricRegistry.name("core.tasksFailed"), (Gauge<Long>) core.getStats()::getTasksFailed);
        metrics.register(MetricRegistry.name("core.tasksAborted"), (Gauge<Long>) core.getStats()::getTasksAborted);
        metrics.register(MetricRegistry.name("core.tasksForwarded"), (Gauge<Long>) core.getStats()::getTasksForwarded);
        metrics.register(MetricRegistry.name("core.tasksDiscarded"), (Gauge<Long>) core.getStats()::getTasksDiscarded);
        metrics.register(MetricRegistry.name("core.currentIdleTime"), (Gauge<Long>) core::getCurrentIdleTime);
        metrics.register(MetricRegistry.name("core.inputSizes"), core.getStats().getInputSizes());
        metrics.register(MetricRegistry.name("core.outputSizes"), core.getStats().getOutputSizes());
    }


    private void initComponentMetrics(final MetricRegistry metrics, final ManagedConfigurationSource config, final ManagedDataStore store, final WorkerCore core)
    {
        metrics.register(MetricRegistry.name("config.lookups"), (Gauge<Integer>) config::getConfigurationRequests);
        metrics.register(MetricRegistry.name("config.errors"), (Gauge<Integer>) config::getConfigurationErrors);
        if ( store.getMetrics() != null ) {
            metrics.register(MetricRegistry.name("store.writes"), (Gauge<Integer>) store.getMetrics()::getStoreRequests);
            metrics.register(MetricRegistry.name("store.reads"), (Gauge<Integer>) store.getMetrics()::getRetrieveRequests);
            metrics.register(MetricRegistry.name("store.deletes"), (Gauge<Integer>) store.getMetrics()::getDeleteRequests);
            metrics.register(MetricRegistry.name("store.errors"), (Gauge<Integer>) store.getMetrics()::getErrors);
        }
        if ( core.getWorkerQueue().getMetrics() != null ) {
            metrics.register(MetricRegistry.name("queue.received"), (Gauge<Integer>) core.getWorkerQueue().getMetrics()::getMessagesReceived);
            metrics.register(MetricRegistry.name("queue.published"), (Gauge<Integer>) core.getWorkerQueue().getMetrics()::getMessagesPublished);
            metrics.register(MetricRegistry.name("queue.rejected"), (Gauge<Integer>) core.getWorkerQueue().getMetrics()::getMessagesRejected);
            metrics.register(MetricRegistry.name("queue.dropped"), (Gauge<Integer>) core.getWorkerQueue().getMetrics()::getMessagesDropped);
            metrics.register(MetricRegistry.name("queue.errors"), (Gauge<Integer>) core.getWorkerQueue().getMetrics()::getQueueErrors);
        }
    }
}
