package com.hpe.caf.worker.testing;

/**
 * Created by ploch on 23/11/2015.
 */
public abstract class SettingsProvider {

    public static final SettingsProvider defaultProvider = new SystemSettingsProvider();

    public abstract String getSetting(String name);

    public boolean getBooleanSetting(String name) {
        return Boolean.parseBoolean(getSetting(name));
    }

}
