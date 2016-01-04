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

        public ValidationSettingsBuilder base64Properties(String... base64Properties) {
            settings.base64Properties = new HashSet<>(Arrays.asList(base64Properties));
            return this;
        }

        public ValidationSettings build(){
            return settings;
        }
    }

    private Set<String> ignoredProperties = new HashSet<>();
    private Set<String> referencedDataProperties = new HashSet<>();
    private Set<String> base64Properties = new HashSet<>();

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

    /**
     * Getter for property 'base64Properties'.
     *
     * @return Value for property 'base64Properties'.
     */
    public Set<String> getBase64Properties() {
        return base64Properties;
    }
}
