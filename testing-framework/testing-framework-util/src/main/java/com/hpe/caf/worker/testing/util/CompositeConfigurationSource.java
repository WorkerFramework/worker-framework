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
package com.hpe.caf.worker.testing.util;

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Created by ploch on 15/02/2017.
 */
public class CompositeConfigurationSource implements ConfigurationSource {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeConfigurationSource.class);

    private final List<ConfigurationSource> configurationSources = new ArrayList<>();

    public CompositeConfigurationSource(ConfigurationSource... configurationSources) {
        if (configurationSources != null) {
            Collections.addAll(this.configurationSources, configurationSources);
        }
    }

    public CompositeConfigurationSource addConfigurationSource(ConfigurationSource configurationSource) {
        configurationSources.add(configurationSource);
        return this;
    }

    public CompositeConfigurationSource insertConfigurationSource(int index, ConfigurationSource configurationSource) {
        configurationSources.add(index, configurationSource);
        return this;
    }

    @Override
    public <T> T getConfiguration(Class<T> aClass) throws ConfigurationException {
        Objects.requireNonNull(aClass);

        for (ConfigurationSource configurationSource : configurationSources) {
            LOG.debug("Trying to retrieve configuration for {} using {}", aClass.getName(), configurationSource.getClass().getName());
            try {
                T configuration = configurationSource.getConfiguration(aClass);
                if (configuration != null) {
                    return configuration;
                }
            } catch (ConfigurationException e) {
                LOG.debug("Failet to retrieve configuration for {} using {}. Exception message: {}", aClass.getName(), configurationSource.getClass().getName(), e.getMessage());
            }
        }
        throw new ConfigurationException("Failed to configuration for " + aClass.getName());
    }
}
