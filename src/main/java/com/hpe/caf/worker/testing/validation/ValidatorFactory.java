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
public class ValidatorFactory {


    private ValidationSettings validationSettings;
    private final DataStore dataStore;
    private final Codec codec;
    private final String testDataFolder;
    private final boolean throwOnNewActualProperty;

    public ValidatorFactory(ValidationSettings validationSettings, DataStore dataStore, Codec codec, TestConfiguration testConfiguration) {
        this.validationSettings = validationSettings;
        this.dataStore = dataStore;
        this.codec = codec;
        this.testDataFolder = testConfiguration.getTestDataFolder();
        this.throwOnNewActualProperty = testConfiguration.throwOnNewActualProperty();
    }

    public PropertyValidator createRootValidator() {
        return new PropertyMapValidator(this);
    }

    public PropertyValidator create(String propertyName, Object sourcePropertyValue, Object validatorPropertyValue) {
        if (propertyName != null) {
            if (validationSettings.getIgnoredProperties().contains(propertyName)) {
                return new IgnorePropertyValidator();
            }
            if (validationSettings.getReferencedDataProperties().contains(propertyName)) {
                return new ReferenceDataValidator(dataStore, codec, testDataFolder);
            }
            if (validationSettings.getArrayReferencedDataProperties().contains(propertyName)){
                return new ArrayReferencedDataValidator(dataStore,codec,testDataFolder);
			}
            if (validationSettings.getUnorderedArrayReferencedDataProperties().contains(propertyName)){
                return new UnorderedArrayReferencedDataValidator(dataStore,codec,testDataFolder);
			}
            if (validationSettings.getBase64Properties().contains(propertyName)) {
                return new Base64PropertyValidator();
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

    public boolean shouldThrowOnNewActualProperty() {
        return this.throwOnNewActualProperty;
    }

}
