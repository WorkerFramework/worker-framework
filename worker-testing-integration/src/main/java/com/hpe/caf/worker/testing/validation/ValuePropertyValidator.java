package com.hpe.caf.worker.testing.validation;

import org.glassfish.jersey.internal.util.Base64;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ploch on 05/12/2015.
 */
public class ValuePropertyValidator extends PropertyValidator {
    @Override
    public boolean isValid(Object testedPropertyValue, Object validatorPropertyValue) {

        if (testedPropertyValue == validatorPropertyValue) return true;

        String testedStringValue = "";

        // If the property to be tested is a byte array, convert it to a string and convert it to Base64
        if (testedPropertyValue instanceof byte[]) {
            byte[] encodedTestedProperty = Base64.encode((byte[]) testedPropertyValue);
            testedStringValue = new String(encodedTestedProperty);
        } else {
            testedStringValue = testedPropertyValue.toString();
        }

        String validationRegex = validatorPropertyValue.toString();

        if (testedStringValue.equals(validationRegex)) return true;

        Pattern pattern = Pattern.compile(validationRegex);
        Matcher matcher = pattern.matcher(testedStringValue);
        boolean match = matcher.matches();
        return match;
    }
}
