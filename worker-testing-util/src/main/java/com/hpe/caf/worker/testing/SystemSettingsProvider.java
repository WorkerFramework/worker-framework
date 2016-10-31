package com.hpe.caf.worker.testing;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

import java.util.Objects;

/**
 * Created by ploch on 23/11/2015.
 */
public class SystemSettingsProvider extends SettingsProvider {

    private final CompositeConfiguration configuration = createConfiguration();

    private static CompositeConfiguration createConfiguration() {
        CompositeConfiguration configuration = new CompositeConfiguration();
        configuration.addConfiguration(new SystemConfiguration());
        configuration.addConfiguration(new EnvironmentConfiguration());
        return configuration;
    }

    public CompositeConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public String getSetting(String name) {
        Objects.requireNonNull(name);
       // return System.getProperty(name, System.getenv(name));
        return configuration.getString(name);
    }

    @Override
    public boolean getBooleanSetting(String name) {
        return configuration.getBoolean(name);
    }

    @Override
    public boolean getBooleanSetting(String name, boolean defaultValue) {
        return configuration.getBoolean(name, defaultValue);
    }
}
