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
package com.hpe.caf.worker.testing.validation;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.configuration.ValidationSettings;

import java.util.Collection;
import java.util.Map;

/**
 * Created by ploch on 07/12/2015.
 */
public class ValidatorFactory
{
    private ValidationSettings validationSettings;
    private final DataStore dataStore;
    private final Codec codec;
    private final String testDataFolder;
    private final boolean failOnUnknownProperty;
    private final String testSourcefileBaseFolder;

    public ValidatorFactory(ValidationSettings validationSettings, DataStore dataStore, Codec codec, TestConfiguration testConfiguration)
    {
        this.validationSettings = validationSettings;
        this.dataStore = dataStore;
        this.codec = codec;
        this.testDataFolder = testConfiguration.getTestDataFolder();
        this.failOnUnknownProperty = testConfiguration.failOnUnknownProperty();
        this.testSourcefileBaseFolder = testConfiguration.getTestSourcefileBaseFolder();
    }

    public PropertyValidator createRootValidator()
    {
        return new PropertyMapValidator(this);
    }

    public PropertyValidator create(String propertyName, Object sourcePropertyValue, Object validatorPropertyValue)
    {
        if (propertyName != null) {
            if (validationSettings.getIgnoredProperties().contains(propertyName)) {
                return new IgnorePropertyValidator();
            }
            if (validationSettings.getReferencedDataProperties().contains(propertyName)) {
                return new ReferenceDataValidator(dataStore, codec, testDataFolder, testSourcefileBaseFolder);
            }
            if (validationSettings.getArrayReferencedDataProperties().contains(propertyName)) {
                return new ArrayReferencedDataValidator(dataStore, codec, testDataFolder, testSourcefileBaseFolder);
            }
            if (validationSettings.getUnorderedArrayReferencedDataProperties().contains(propertyName)) {
                return new UnorderedArrayReferencedDataValidator(dataStore, codec, testDataFolder, testSourcefileBaseFolder);
            }
            if (validationSettings.getBase64Properties().contains(propertyName)) {
                return new Base64PropertyValidator();
            }
        }
        for (final CustomPropertyValidator customPropertyValidator : validationSettings.getCustomValidators()) {
            if (customPropertyValidator.canValidate(propertyName, sourcePropertyValue, validatorPropertyValue)) {
                return customPropertyValidator;
            }
        }
        if (sourcePropertyValue instanceof Map && validatorPropertyValue instanceof Map) {
            return new PropertyMapValidator(this);
        }

        if (sourcePropertyValue instanceof Collection && validatorPropertyValue instanceof Collection) {
            return new CollectionValidator(this);
        }

        return new ValuePropertyValidator();

    }

    public boolean shouldFailOnUnknownProperty()
    {
        return this.failOnUnknownProperty;
    }
}
