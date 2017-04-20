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

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by ploch on 17/03/2017.
 */
public class CompositeSettingsProvider extends SettingsProvider
{

    private final Collection<SettingsProvider> settingsProviders = new ArrayList<>();

    public CompositeSettingsProvider(SettingsProvider... settingsProviders)
    {

        Collections.addAll(this.settingsProviders, settingsProviders);
    }

    @Override
    protected String doGetSetting(String name)
    {
        for (SettingsProvider settingsProvider : settingsProviders) {
            String setting = settingsProvider.getSetting(name);
            if (!Strings.isNullOrEmpty(setting)) {
                return setting;
            }
        }
        return null;
    }
}
