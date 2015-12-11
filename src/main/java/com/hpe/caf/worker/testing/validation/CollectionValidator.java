package com.hpe.caf.worker.testing.validation;

import java.util.Collection;

/**
 * Created by ploch on 08/12/2015.
 */
public class CollectionValidator extends PropertyValidator {

    private final ValidatorFactory validatorFactory;

    public CollectionValidator(ValidatorFactory validatorFactory) {

        this.validatorFactory = validatorFactory;
    }

    @Override
    protected boolean isValid(Object testedPropertyValue, Object validatorPropertyValue) {
        if (!(testedPropertyValue instanceof Collection)) return false;
        if (!(validatorPropertyValue instanceof Collection)) return false;

        Object[] testedArray = ((Collection) testedPropertyValue).toArray();

        Object[] validationArray = ((Collection) validatorPropertyValue).toArray();

        for (int i = 0; i < validationArray.length; i++){
            Object testedValue = testedArray[i];
            Object validationValue = validationArray[i];
            PropertyValidator validator = validatorFactory.create(null, testedValue, validationValue);
            boolean valid = validator.isValid(testedValue, validationValue);
            if (!valid) return false;
        }

        return true;
    }
}
