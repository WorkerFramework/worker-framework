/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.worker.testing.validation;

import java.util.Base64;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ploch on 05/12/2015.
 */
public class ValuePropertyValidator extends PropertyValidator
{
    private static final Logger LOG = LoggerFactory.getLogger(ValuePropertyValidator.class);
    @Override
    public boolean isValid(Object testedPropertyValue, Object validatorPropertyValue)
    {

        if (testedPropertyValue == validatorPropertyValue) {
            return true;
        }

        String testedStringValue = "";

        // If the property to be tested is a byte array, convert it to a string and convert it to Base64
        if (testedPropertyValue instanceof byte[]) {
            byte[] encodedTestedProperty = Base64.getEncoder().encode((byte[]) testedPropertyValue);
            testedStringValue = new String(encodedTestedProperty);
        } else {
            testedStringValue = testedPropertyValue.toString();
        }

        String validationRegex = validatorPropertyValue.toString();

        if (testedStringValue.equals(validationRegex)) {
            return true;
        }

        final Pattern pattern;
        try {
            pattern = Pattern.compile(validationRegex);
        }
        catch (PatternSyntaxException e) {
            LOG.debug(" Validation string is not a valid regex. " + e.getMessage());
            return false;
        }
        Matcher matcher = pattern.matcher(testedStringValue);
        boolean match = matcher.matches();
        return match;
    }
}
