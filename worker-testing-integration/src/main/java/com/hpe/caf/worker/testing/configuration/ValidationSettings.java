/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.testing.configuration;

import com.hpe.caf.worker.testing.validation.CustomPropertyValidator;

import java.util.*;

/**
 * Created by ploch on 07/12/2015.
 */
public class ValidationSettings
{
    public static class ValidationSettingsBuilder
    {
        private final ValidationSettings settings;

        private ValidationSettingsBuilder(ValidationSettings settings)
        {
            this.settings = settings;
        }

        public ValidationSettingsBuilder ignoreProperties(String... ignoredProperties)
        {
            settings.ignoredProperties = new HashSet<>(Arrays.asList(ignoredProperties));
            return this;
        }

        public ValidationSettingsBuilder referencedDataProperties(String... referencedDataProperties)
        {
            settings.referencedDataProperties = new HashSet<>(Arrays.asList(referencedDataProperties));
            return this;
        }

        public ValidationSettingsBuilder arrayReferencedDataProperties(String... arrayReferencedDataProperties)
        {
            settings.arrayReferencedDataProperties = new HashSet<>(Arrays.asList(arrayReferencedDataProperties));
            return this;
        }

        public ValidationSettingsBuilder unorderedArrayReferencedDataProperties(String... unorderedArrayReferencedDataProperties)
        {
            settings.unorderedArrayReferencedDataProperties = new HashSet<>(Arrays.asList(unorderedArrayReferencedDataProperties));
            return this;
        }

        public ValidationSettingsBuilder base64Properties(String... base64Properties)
        {
            settings.base64Properties = new HashSet<>(Arrays.asList(base64Properties));
            return this;
        }

        public ValidationSettingsBuilder customValidators(CustomPropertyValidator... customPropertyValidators)
        {
            settings.customValidators = Arrays.asList(customPropertyValidators);
            return this;
        }

        public ValidationSettings build()
        {
            return settings;
        }
    }

    private Set<String> arrayReferencedDataProperties = new HashSet<>();
    private Set<String> unorderedArrayReferencedDataProperties = new HashSet<>();
    private Set<String> ignoredProperties = new HashSet<>();
    private Set<String> referencedDataProperties = new HashSet<>();
    private Set<String> base64Properties = new HashSet<>();
    private List<CustomPropertyValidator> customValidators = new ArrayList<>();

    public static ValidationSettingsBuilder configure()
    {
        return new ValidationSettingsBuilder(new ValidationSettings());

    }

    public ValidationSettings()
    {
    }

    /**
     * Getter for property 'ignoredProperties'.
     *
     * @return Value for property 'ignoredProperties'.
     */
    public Set<String> getIgnoredProperties()
    {
        return ignoredProperties;
    }

    /**
     * Getter for property 'referenceDataProperties'.
     *
     * @return Value for property 'referenceDataProperties'.
     */
    public Set<String> getReferencedDataProperties()
    {
        return referencedDataProperties;
    }

    public Set<String> getArrayReferencedDataProperties()
    {
        return arrayReferencedDataProperties;
    }

    public Set<String> getUnorderedArrayReferencedDataProperties()
    {
        return unorderedArrayReferencedDataProperties;
    }

    /**
     * Getter for property 'base64Properties'.
     *
     * @return Value for property 'base64Properties'.
     */
    public Set<String> getBase64Properties()
    {
        return base64Properties;
    }

    public List<CustomPropertyValidator> getCustomValidators()
    {
        return customValidators;
    }
}
