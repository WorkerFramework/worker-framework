package com.hpe.caf.worker.testing.validation;

import com.hpe.caf.worker.testing.configuration.ValidationSettings;

import java.util.Map;

/**
 * Created by ploch on 07/12/2015.
 */
public class ValidatorFactory {


    private ValidationSettings validationSettings;

    public ValidatorFactory(ValidationSettings validationSettings) {
        this.validationSettings = validationSettings;
    }

    public PropertyValidator createRootValidator() {
        return new PropertyMapValidator(this);
    }

    public PropertyValidator create(String propertyName, Object sourcePropertyValue, Object validatorPropertyValue) {
        if (validationSettings.getIgnoredProperties().contains(propertyName)) {
            return new IgnorePropertyValidator();
        }
        if (validationSettings.getReferencedDataProperties().contains(propertyName)) {
            return new ReferenceDataValidator();
        }

        if (sourcePropertyValue instanceof Map && validatorPropertyValue instanceof Map) {
            return new PropertyMapValidator(this);
        }

        return new ValuePropertyValidator();

    }

}
