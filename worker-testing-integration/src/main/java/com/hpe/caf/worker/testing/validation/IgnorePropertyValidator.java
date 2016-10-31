package com.hpe.caf.worker.testing.validation;

/**
 * Created by ploch on 07/12/2015.
 */
public class IgnorePropertyValidator extends PropertyValidator {
    @Override
    public boolean isValid(Object testedPropertyValue, Object validatorPropertyValue) {
        return true;
    }
}
