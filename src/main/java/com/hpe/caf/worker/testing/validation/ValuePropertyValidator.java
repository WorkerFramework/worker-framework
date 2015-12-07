package com.hpe.caf.worker.testing.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ploch on 05/12/2015.
 */
public class ValuePropertyValidator extends PropertyValidator {
    @Override
    public boolean isValid(Object testedPropertyValue, Object validatorPropertyValue) {

        if (testedPropertyValue == validatorPropertyValue) return true;

        String testedStringValue = testedPropertyValue.toString();
        String validationRegex = validatorPropertyValue.toString();

        if (testedStringValue.equals(validationRegex)) return true;

        Pattern pattern = Pattern.compile(validationRegex);
        Matcher matcher = pattern.matcher(testedStringValue);
        boolean match = matcher.matches();
        return match;
    }
}
