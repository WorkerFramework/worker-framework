package com.hpe.caf.worker.testing.validation;

/**
 * Created by ploch on 05/12/2015.
 */
public abstract class PropertyValidator {

    protected abstract boolean isValid(Object testedPropertyValue, Object validatorPropertyValue);

    public void validate(String propertyName, Object testedPropertyValue, Object validatorPropertyValue){
        boolean isValid = isValid(testedPropertyValue, validatorPropertyValue);

        if (!isValid) {
            throw new AssertionError(String.format("Validator name: %s.\nValidation of property '%s' failed.\n Actual property value: %s\n Validation string: %s", getClass().getSimpleName(), propertyName, testedPropertyValue, validatorPropertyValue));
        }
    }
}
