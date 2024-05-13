/*
 * Copyright 2015-2024 Open Text.
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
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.ManagedConfigurationSource;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.cipher.NullCipherProvider;
import com.hpe.caf.config.system.SystemBootstrapConfiguration;
import com.hpe.caf.configs.HealthConfiguration;
import com.hpe.caf.naming.ServicePath;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;

import ch.qos.logback.classic.util.DefaultJoranConfigurator;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.health.DefaultHealthFactory;
import io.dropwizard.health.HealthCheckConfiguration;
import io.dropwizard.health.HealthCheckType;
import io.dropwizard.health.Schedule;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.logging.common.LoggingUtil;
import io.dropwizard.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ResponseCache;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * This is the main HP SaaS asynchronous micro-service worker entry point. On startup, it will identify implementations of necessary
 * components (such as a queue, data store, and backend worker), and create a WorkerCore object which will handle the logic of data flow
 * between these components. The application class itself here just wrappers this and exposes health checks and metrics.
 */
public final class WorkerApplication extends Application<WorkerConfiguration>
{
    private final long startTime = System.currentTimeMillis();
    private static final Logger LOG = LoggerFactory.getLogger(WorkerApplication.class);

    /**
     * Entry point for the asynchronous micro-service worker framework.
     *
     * @param args command-line args, currently not used by the worker
     * @throws Exception if the worker cannot startup
     */
    public static void main(final String[] args)
        throws Exception
    {
        new WorkerApplication().run(args);
    }

    /**
     * Package private. If you wish to test a worker framework, try using WorkerCore.
     */
    WorkerApplication()
    {
    }

    /**
     * Start the asynchronous worker micro-service.
     *
     * @param workerConfiguration the internal worker service internal configuration
     * @param environment the dropwizard environment, for setting up metrics and REST configuration
     */
    @Override
    public void run(final WorkerConfiguration workerConfiguration, final Environment environment)
        throws QueueException, ModuleLoaderException, CipherException, ConfigurationException, DataStoreException, WorkerException
    {
        LOG.info("Worker initializing.");

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

        TransientHealthCheck transientHealthCheck = new TransientHealthCheck();
        WorkerCore core = new WorkerCore(codec, wtp, workerQueue, workerFactory, path, environment.healthChecks(), transientHealthCheck);
        HealthConfiguration healthConfiguration = config.getConfiguration(HealthConfiguration.class);

        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() {
                LOG.info("Worker starting up.");

                initCoreMetrics(environment.metrics(), core);
                initComponentMetrics(environment.metrics(), config, store, core);
                initHealthChecks(workerQueue, core, healthConfiguration, config, environment, workerFactory, store,
                        transientHealthCheck, workerConfiguration);
            }

            @Override
            public void stop() {
                LOG.info("Worker stop requested, allowing in-progress tasks to complete.");
                workerQueue.shutdownIncoming();
                while (!wtp.isIdle()) {
                    try {
                        //The grace period will expire and the process killed so no need for time limit here
                        LOG.trace("Awaiting the Worker Thread Pool to become idle, {} tasks in the backlog.", 
                                wtp.getBacklogSize());
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
                LOG.trace("Worker Thread Pool is idle.");
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
                LOG.info("Worker stopped.");
            }
        });
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
        metrics.register(MetricRegistry.name("core.tasksPaused"), (Gauge<Long>) core.getStats()::getTasksPaused);
        metrics.register(MetricRegistry.name("core.tasksDiscarded"), (Gauge<Long>) core.getStats()::getTasksDiscarded);
        metrics.register(MetricRegistry.name("core.currentIdleTime"), (Gauge<Long>) core::getCurrentIdleTime);
        metrics.register(MetricRegistry.name("core.inputSizes"), core.getStats().getInputSizes());
        metrics.register(MetricRegistry.name("core.outputSizes"), core.getStats().getOutputSizes());
    }

    private void initComponentMetrics(final MetricRegistry metrics, final ManagedConfigurationSource config, final ManagedDataStore store, final WorkerCore core)
    {
        metrics.register(MetricRegistry.name("config.lookups"), (Gauge<Integer>) config::getConfigurationRequests);
        metrics.register(MetricRegistry.name("config.errors"), (Gauge<Integer>) config::getConfigurationErrors);
        if (store.getMetrics() != null) {
            metrics.register(MetricRegistry.name("store.writes"), (Gauge<Integer>) store.getMetrics()::getStoreRequests);
            metrics.register(MetricRegistry.name("store.reads"), (Gauge<Integer>) store.getMetrics()::getRetrieveRequests);
            metrics.register(MetricRegistry.name("store.deletes"), (Gauge<Integer>) store.getMetrics()::getDeleteRequests);
            metrics.register(MetricRegistry.name("store.errors"), (Gauge<Integer>) store.getMetrics()::getErrors);
        }
        if (core.getWorkerQueue().getMetrics() != null) {
            metrics.register(MetricRegistry.name("queue.received"), (Gauge<Integer>) core.getWorkerQueue().getMetrics()::getMessagesReceived);
            metrics.register(MetricRegistry.name("queue.published"), (Gauge<Integer>) core.getWorkerQueue().getMetrics()::getMessagesPublished);
            metrics.register(MetricRegistry.name("queue.rejected"), (Gauge<Integer>) core.getWorkerQueue().getMetrics()::getMessagesRejected);
            metrics.register(MetricRegistry.name("queue.dropped"), (Gauge<Integer>) core.getWorkerQueue().getMetrics()::getMessagesDropped);
            metrics.register(MetricRegistry.name("queue.errors"), (Gauge<Integer>) core.getWorkerQueue().getMetrics()::getQueueErrors);
        }
    }

    private void initHealthChecks(
            final ManagedWorkerQueue workerQueue,
            final WorkerCore workerCore,
            final HealthConfiguration healthConfiguration,
            final ManagedConfigurationSource managedConfigurationSource,
            final Environment environment,
            final WorkerFactory workerFactory,
            final ManagedDataStore managedDataStore,
            final TransientHealthCheck transientHealthCheck,
            final WorkerConfiguration workerConfiguration)
    {
        // How health checks work:
        //
        // 1)
        //
        // Registering health checks via environment.healthChecks().register(...) results in them being returned when calling:
        //
        // localhost:8081/healthcheck
        //
        // i.e. calling localhost:8081/healthcheck results in all the registered health checks (i.e. all the liveness and readiness
        // checks) getting run synchronously.
        //
        // 2)
        //
        // Configuring those health checks via the healthFactory.setHealthCheckConfigurations(...) results in them being run on a
        // schedule and *also* being returned when calling:
        //
        // localhost:8080/health-check?name=all&type=ALIVE OR
        // localhost:8080/health-check?name=all&type=READY
        //
        // i.e. calling localhost:8080/health-check?name=all&type=ALIVE results in getting the last result of the scheduled liveness
        // checks (it can be thought of as an asynchronous call).
        //
        // 3)
        //
        // Due to the above, although both localhost:8081/healthcheck and localhost:8080/health-check?name=all&type=READY will
        // return the result of the readiness checks (which also include liveness checks), the response codes of these calls may
        // be different.

        final GatedHealthProvider gatedHealthProvider = new GatedHealthProvider(workerQueue, workerCore);

        final List<HealthCheckConfiguration> healthCheckConfigurations = new ArrayList<>();

        /////////////////////////////
        // Liveness Checks
        /////////////////////////////

        final Schedule livenessSchedule = createSchedule(
                healthConfiguration.getLivenessInitialDelaySeconds(),
                healthConfiguration.getLivenessCheckIntervalSeconds(),
                healthConfiguration.getLivenessDowntimeIntervalSeconds(),
                healthConfiguration.getLivenessSuccessAttempts(),
                healthConfiguration.getLivenessFailureAttempts());

        LOG.debug("Liveness checks will be run on the following schedule: " +
                        "initialDelay={}, checkInterval={}, downtimeInterval={}, successAttempts={}, failureAttempts={}",
                livenessSchedule.getInitialDelay(), livenessSchedule.getCheckInterval(), livenessSchedule.getDowntimeInterval(),
                livenessSchedule.getSuccessAttempts(), livenessSchedule.getFailureAttempts());

        registerHealthCheck("worker-alive", workerFactory::livenessCheck, environment, gatedHealthProvider);
        healthCheckConfigurations.add(createHealthCheckConfiguration("worker-alive", HealthCheckType.ALIVE, livenessSchedule));

        // deadlocks is supplied by default by Dropwizard, we don't need to register it
        healthCheckConfigurations.add(createHealthCheckConfiguration("deadlocks", HealthCheckType.ALIVE, livenessSchedule));

        registerHealthCheck("queue", workerCore.getWorkerQueue()::livenessCheck, environment, gatedHealthProvider);
        healthCheckConfigurations.add(createHealthCheckConfiguration("queue", HealthCheckType.ALIVE, livenessSchedule));

        /////////////////////////////
        // Readiness Checks
        /////////////////////////////

        final Schedule readinessSchedule = createSchedule(
                healthConfiguration.getReadinessInitialDelaySeconds(),
                healthConfiguration.getReadinessCheckIntervalSeconds(),
                healthConfiguration.getReadinessDowntimeIntervalSeconds(),
                healthConfiguration.getReadinessSuccessAttempts(),
                healthConfiguration.getReadinessFailureAttempts());

        LOG.debug("Readiness checks will be run on the following schedule: " +
                        "initialDelay={}, checkInterval={}, downtimeInterval={}, successAttempts={}, failureAttempts={}",
                readinessSchedule.getInitialDelay(), readinessSchedule.getCheckInterval(), readinessSchedule.getDowntimeInterval(),
                readinessSchedule.getSuccessAttempts(), readinessSchedule.getFailureAttempts());

        registerHealthCheck("worker-ready", workerFactory::healthCheck, environment, gatedHealthProvider);
        healthCheckConfigurations.add(createHealthCheckConfiguration("worker-ready", HealthCheckType.READY, readinessSchedule));

        registerHealthCheck("configuration", managedConfigurationSource::healthCheck, environment, gatedHealthProvider);
        healthCheckConfigurations.add(createHealthCheckConfiguration("configuration", HealthCheckType.READY, readinessSchedule));

        registerHealthCheck("store", managedDataStore::healthCheck, environment, gatedHealthProvider);
        healthCheckConfigurations.add(createHealthCheckConfiguration("store", HealthCheckType.READY, readinessSchedule));

        registerHealthCheck("transient", transientHealthCheck::healthCheck, environment, gatedHealthProvider);
        healthCheckConfigurations.add(createHealthCheckConfiguration("transient", HealthCheckType.READY, readinessSchedule));

        /////////////////////////////
        // HealthFactory Creation
        /////////////////////////////

        final DefaultHealthFactory healthFactory = new DefaultHealthFactory();

        healthFactory.setHealthCheckConfigurations(healthCheckConfigurations);
        healthFactory.configure(environment.lifecycle(), environment.servlets(), environment.jersey(), environment.health(),
                environment.getObjectMapper(), getName());

        workerConfiguration.setHealthFactory(healthFactory);
    }

    @Override
    protected void bootstrapLogging() {
        // If logback.xml is present, prevent dropwizard from overriding it
        // dropwizard overrides it and reads logging configuration from the dropwizard yml instead, because, one of its
        // offerings as a framework is to provide a single configuration point
        if (WorkerApplication.shouldBootstrapLogging()) {
            // logback.xml is not present, let dropwizard continue bootstrap logging
            super.bootstrapLogging();
        } else {
            // This should allow logback.xml to be used by logback-classic
            // Gets the root j.u.l.Logger and removes all registered handlers
            // then redirects all active j.u.l. to SLF4J
            LoggingUtil.hijackJDKLogging();
        }
    }

    private static boolean shouldBootstrapLogging() {
        final DefaultJoranConfigurator djc = new DefaultJoranConfigurator();
        final URL url = djc.findURLOfDefaultConfigurationFile(false);
        return url == null;
    }

    private static Schedule createSchedule(
            final int initialDelaySeconds,
            final int checkIntervalSeconds,
            final int downtimeIntervalSeconds,
            final int successAttempts,
            final int failureAttempts) {
        final Schedule schedule = new Schedule();

        schedule.setInitialDelay(Duration.seconds(initialDelaySeconds));
        schedule.setCheckInterval(Duration.seconds(checkIntervalSeconds));
        schedule.setDowntimeInterval(Duration.seconds(downtimeIntervalSeconds));
        schedule.setSuccessAttempts(successAttempts);
        schedule.setFailureAttempts(failureAttempts);

        return schedule;
    }

    private static void registerHealthCheck(
            final String name,
            final Supplier<HealthResult> healthResultSupplier,
            final Environment environment,
            final GatedHealthProvider gatedHealthProvider)
    {
        environment.healthChecks().register(
                name,
                gatedHealthProvider.new GatedHealthCheck(name, new WorkerHealthCheck(healthResultSupplier)));
    }

    private static HealthCheckConfiguration createHealthCheckConfiguration(
            final String name,
            final HealthCheckType healthCheckType,
            final Schedule schedule) {
        final HealthCheckConfiguration healthCheckConfiguration = new HealthCheckConfiguration();

        healthCheckConfiguration.setName(name);
        healthCheckConfiguration.setType(healthCheckType);
        healthCheckConfiguration.setInitialState(false);
        healthCheckConfiguration.setSchedule(schedule);

        // Setting critical to false means that the /health-check endpoint returns HTTP 200 even if a healthcheck fails, which is
        // not desired. Setting critical to true means that the /health-check endpoint returns HTTP 503 when a healthcheck fails.
        healthCheckConfiguration.setCritical(true);

        return healthCheckConfiguration;
    }
}
