package com.hpe.caf.worker.testing;

import com.sun.org.apache.bcel.internal.generic.NEW;

/**
 * Created by ploch on 23/11/2015.
 */
public abstract class SettingsProvider {

    public static final SettingsProvider defaultProvider = new SystemSettingsProvider();

    public abstract String getSetting(String name);
}
