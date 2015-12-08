package com.hpe.caf.worker.testing.configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ploch on 07/12/2015.
 */
public class ValidationSettings {

    public static class ValidationSettingsBuilder {

        private final ValidationSettings settings;

        private ValidationSettingsBuilder(ValidationSettings settings) {
            this.settings = settings;
        }

        public ValidationSettingsBuilder ignoreProperties(String... ignoredProperties) {
            settings.ignoredProperties = new HashSet<>(Arrays.asList(ignoredProperties));
            return this;
        }

        public ValidationSettingsBuilder referencedDataProperties(String... referencedDataProperties) {
            settings.referencedDataProperties = new HashSet<>(Arrays.asList(referencedDataProperties));
            return this;
        }

        public ValidationSettings build(){
            return settings;
        }
    }

    private Set<String> ignoredProperties;
    private Set<String> referencedDataProperties;

    public static ValidationSettingsBuilder configure() {
        return new ValidationSettingsBuilder(new ValidationSettings());

    }

    public ValidationSettings(){}


    /**
     * Getter for property 'ignoredProperties'.
     *
     * @return Value for property 'ignoredProperties'.
     */
    public Set<String> getIgnoredProperties() {
        return ignoredProperties;
    }

    /**
     * Getter for property 'referenceDataProperties'.
     *
     * @return Value for property 'referenceDataProperties'.
     */
    public Set<String> getReferencedDataProperties() {
        return referencedDataProperties;
    }
}
