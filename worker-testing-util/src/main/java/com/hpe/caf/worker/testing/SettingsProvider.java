package com.hpe.caf.worker.testing;

/**
 * Created by ploch on 23/11/2015.
 */
//// TODO: 25/12/2015 Remove this class and use commons-configuration Configuration class instead.
public abstract class SettingsProvider {

    public static final SystemSettingsProvider defaultProvider = new SystemSettingsProvider();

    public abstract String getSetting(String name);

    public boolean getBooleanSetting(String name) {
        return Boolean.parseBoolean(getSetting(name));
    }

    public boolean getBooleanSetting(String name, boolean defaultValue) {
        String setting = getSetting(name);
        if (setting != null) {
            return Boolean.parseBoolean(getSetting(name));
        }
        return defaultValue;
    }

}
