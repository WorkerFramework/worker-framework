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

import java.util.HashMap;
import java.util.Objects;


/**
 * The configuration source which allows to set configuration classes in code.
 */
public class CodeConfigurationSource implements ConfigurationSource {

    private final HashMap<Class, Object> configurations = new HashMap<>();

    /**
     * Instantiates a new CodeConfigurationSource.
     */
    public CodeConfigurationSource() {
    }

    /**
     * Instantiates a new CodeConfigurationSource with supplied configuration objects.
     *
     * @param configurations the configurations
     * @throws IllegalArgumentException if multiple configurations of the same type are passed.
     */
    public CodeConfigurationSource(Object... configurations) {
        Objects.requireNonNull(configurations);

        for (Object configuration : configurations) {
            addConfiguration(configuration, false);
        }
    }

    /**
     * Retrieves a configuration object.
     * @param configClass the class that represents your configuration
     * @param <T>
     * @return configuration object.
     * @throws ConfigurationException
     */
    @Override
    public <T> T getConfiguration(Class<T> configClass) throws ConfigurationException {
        Objects.requireNonNull(configClass);
        if (!configurations.containsKey(configClass)) {
            throw new ConfigurationException("Could not find " + configClass.getName());
        }
        return (T) configurations.get(configClass);
    }

    /**
     * Add a new configuration object to the source.
     *
     * @param configuration the configuration
     * @return the code configuration source
     * @throws IllegalArgumentException if this configuration source already contains configuration object of the same type.
     */
    public CodeConfigurationSource addConfiguration(Object configuration) {
        return addConfiguration(configuration, false);
    }

    /**
     * Adds or replaces a configuration object in the configuration source.
     *
     * @param configuration    to add.
     * @param overrideIfExists if true, it will replace existing configuration object, if false, method will throw                         IllegalArgumentException in case of existing class.
     * @return this CodeConfigurationSource instance.
     */
    public CodeConfigurationSource addConfiguration(Object configuration, boolean overrideIfExists) {
        Class<?> configurationClass = configuration.getClass();
        if (!overrideIfExists && configurations.containsKey(configurationClass)) {
            throw new IllegalArgumentException(String.format("Configuration for %s already set.", configurationClass.getName()));
        }
        configurations.put(configurationClass, configuration);

        return this;
    }
}
