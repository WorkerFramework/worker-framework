package com.hpe.caf.worker.testing.validation;

import org.apache.commons.codec.binary.Base64;

import java.util.Arrays;

/**
 * The {@code Base64PropertyValidator} class.
 * If a validated property is {@code byte[]} then the expected result will be
 * serialized as Base64 string. This validator converts Base64 string to byte array
 * and then compares source property value (which has to be a byte array) with the expected.
 */
public class Base64PropertyValidator extends PropertyValidator {
    @Override
    protected boolean isValid(Object testedPropertyValue, Object validatorPropertyValue) {
        if (!(testedPropertyValue instanceof byte[]) || !(validatorPropertyValue instanceof String)) {
            throw new AssertionError("Unexpected types provided to Base64PropertyValidator. Expected byte array testedPropertyValue and String validatorPropertyValue. Provided were "
                    + testedPropertyValue.getClass().getSimpleName() + " and " + validatorPropertyValue.getClass().getSimpleName() +
                    ". Values: " + testedPropertyValue.toString() + ", " + validatorPropertyValue.toString());
        }


        return Arrays.equals((byte[])testedPropertyValue, Base64.decodeBase64(validatorPropertyValue.toString()));
    }
}
