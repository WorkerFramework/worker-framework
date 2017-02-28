/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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

import com.google.common.base.Strings;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.configuration.ValidationSettings;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

public class CustomPropertyValidatorTest {

    /**
     * Tests "happy-path" custom validation.
     */
    @Test
    public void testCustomPropertyValidation() {
        final ValidationSettings validationSettings = ValidationSettings.configure()
                .customValidators(new CustomIntByNamePropertyValidator("intProp1", "intProp2"), new CustomStringPropertyValidator())
                .build();

        final ValidatorFactory validatorFactory = new ValidatorFactory(validationSettings, null, null, TestConfiguration.createDefault(null, null, null, null));

        final PropertyValidator namedIntProp1Validator = validatorFactory.create("intProp1", 42, 42);
        assertTrue(namedIntProp1Validator.isValid(42, 42));
        final PropertyValidator namedIntProp2Validator = validatorFactory.create("intProp2", 999, 999);
        assertTrue(namedIntProp2Validator.isValid(999, 999));
        assertEquals(namedIntProp1Validator, namedIntProp2Validator, "Expected the same validator to be used for both unnamed int property validations");

        final PropertyValidator unnamedIntPropValidator = validatorFactory.create(null, 42, 42);
        assertTrue(unnamedIntPropValidator.isValid(42, 42));
        assertNotEquals(namedIntProp1Validator, unnamedIntPropValidator, "Expected different validators to be used for named and unnamed int property validations");

        final PropertyValidator stringProp1Validator = validatorFactory.create("stringProp1", "hello world", "hello world");
        assertTrue(stringProp1Validator.isValid("hello world", "hello world"));
        final PropertyValidator stringProp2Validator = validatorFactory.create(null, "go away", "go away");
        assertTrue(stringProp2Validator.isValid("go away", "go away"));
        assertEquals(stringProp1Validator, stringProp2Validator, "Expected the same validator to be used for named and unnamed string property validations");
    }


    private static class CustomIntByNamePropertyValidator extends CustomPropertyValidator {
        private Collection<String> recognizedPropertyNames = new ArrayList<>();

        public CustomIntByNamePropertyValidator(final String... recognizedPropertyNames) {
            this.recognizedPropertyNames = new ArrayList<>(Arrays.asList(recognizedPropertyNames));
        }

        @Override
        public boolean canValidate(String propertyName, Object sourcePropertyValue, Object validatorPropertyValue) {
            return !Strings.isNullOrEmpty(propertyName) &&
                    recognizedPropertyNames.contains(propertyName) &&
                    sourcePropertyValue instanceof Integer &&
                    validatorPropertyValue instanceof Integer;
        }

        @Override
        protected boolean isValid(Object testedPropertyValue, Object validatorPropertyValue) {
            return testedPropertyValue instanceof Integer &&
                    validatorPropertyValue instanceof Integer &&
                    testedPropertyValue.equals(validatorPropertyValue);
        }
    }


    private static class CustomStringPropertyValidator extends CustomPropertyValidator {
        @Override
        public boolean canValidate(String propertyName, Object sourcePropertyValue, Object validatorPropertyValue) {
            return sourcePropertyValue instanceof String &&
                    validatorPropertyValue instanceof String;
        }

        @Override
        protected boolean isValid(Object testedPropertyValue, Object validatorPropertyValue) {
            return testedPropertyValue instanceof String &&
                    validatorPropertyValue instanceof String &&
                    testedPropertyValue.equals(validatorPropertyValue);
        }
    }
}
