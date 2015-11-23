package com.hpe.caf.worker.testing;

import java.util.Objects;

/**
 * Created by ploch on 23/11/2015.
 */
public class SystemSettingsProvider extends SettingsProvider {

    @Override
    public String getSetting(String name) {
        Objects.requireNonNull(name);
        return System.getProperty(name, System.getenv(name));
    }

}
