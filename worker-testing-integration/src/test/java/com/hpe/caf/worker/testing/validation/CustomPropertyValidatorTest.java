/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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

import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.configuration.ValidationSettings;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.testng.Assert.*;

public class CustomPropertyValidatorTest
{
    /**
     * Tests "happy-path" custom validation.
     */
    @Test
    public void testCustomValidatorsUsed()
    {
        final String INT_PROP_1 = "intProp1";
        final String INT_PROP_2 = "intProp2";
        final String STRING_PROP_1 = "stringProp1";

        final ValidationSettings validationSettings = ValidationSettings.configure()
            .customValidators(new CustomIntPropertyValidator(INT_PROP_1, INT_PROP_2),
                              new CustomIntPropertyValidator(),
                              new CustomStringPropertyValidator())
            .build();

        final ValidatorFactory validatorFactory
            = new ValidatorFactory(validationSettings, null, null, TestConfiguration.createDefault(null, null, null, null));

        final PropertyValidator namedIntProp1Validator = validatorFactory.create(INT_PROP_1, 42, 42);
        assertTrue(namedIntProp1Validator instanceof CustomPropertyValidator,
                   "Expected a CustomPropertyValidator as we've configured a CustomIntPropertyValidator that accepts the name of "
                   + "the property being validated");
        assertTrue(namedIntProp1Validator.isValid(42, 42));

        final PropertyValidator namedIntProp2Validator = validatorFactory.create(INT_PROP_2, 999, 999);
        assertTrue(namedIntProp2Validator instanceof CustomPropertyValidator,
                   "Expected a CustomPropertyValidator as we've configured a CustomIntPropertyValidator that accepts the name of "
                   + "the property being validated");
        assertTrue(namedIntProp2Validator.isValid(999, 999));

        assertEquals(namedIntProp1Validator, namedIntProp2Validator,
                     "Expected the same validator instance to be used for both named int property validations as we've configured a "
                     + "CustomIntPropertyValidator that accepts the names of both properties being validated");

        final PropertyValidator unnamedIntPropValidator = validatorFactory.create(null, 42, 42);
        assertTrue(unnamedIntPropValidator instanceof CustomPropertyValidator,
                   "Expected a CustomPropertyValidator as we've configured a CustomIntPropertyValidator that ignores the name of "
                   + "the property being validated");
        assertTrue(unnamedIntPropValidator.isValid(42, 42));

        assertNotEquals(namedIntProp1Validator, unnamedIntPropValidator,
                        "Expected different validator instances for named and unnamed int property validations");

        final PropertyValidator namedStringPropValidator = validatorFactory.create(STRING_PROP_1, "hello world", "hello world");
        assertTrue(namedStringPropValidator instanceof CustomPropertyValidator,
                   "Expected a CustomPropertyValidator as we've configured a CustomStringPropertyValidator that ignores the name of "
                   + "the property being validated");
        assertTrue(namedStringPropValidator.isValid("hello world", "hello world"));

        final PropertyValidator unnamedStringPropValidator = validatorFactory.create(null, "go away", "go away");
        assertTrue(unnamedStringPropValidator instanceof CustomPropertyValidator,
                   "Expected a CustomPropertyValidator as we've configured a CustomStringPropertyValidator that ignores the name of "
                   + "the property being validated");
        assertTrue(unnamedStringPropValidator.isValid("go away", "go away"));

        assertEquals(namedStringPropValidator, unnamedStringPropValidator,
                     "Expected the same validator instance to be used for both named and unnamed string property validations as we've "
                     + "configured a CustomStringPropertyValidator that ignores the names of the property being validated");
    }

    @Test
    public void testCustomValidatorCreatedWithDifferingPropertyValues()
    {
        final String INT_PROP_1 = "intProp1";

        final ValidationSettings validationSettings = ValidationSettings.configure()
            .customValidators(new CustomIntPropertyValidator(INT_PROP_1))
            .build();

        final ValidatorFactory validatorFactory
            = new ValidatorFactory(validationSettings, null, null, TestConfiguration.createDefault(null, null, null, null));

        final PropertyValidator validator = validatorFactory.create(INT_PROP_1, 42, 53);
        assertTrue(validator instanceof CustomPropertyValidator,
                   "Expected a CustomPropertyValidator for named int property validation despite specifying differing values when "
                   + "creating the validator, as we've configured a CustomIntPropertyValidator that accepts the name of the "
                   + "property being validated");
        assertFalse(validator.isValid(42, 53));
    }

    @Test
    public void testCustomValidatorCreatedWithDifferingUnnamedPropertyValues()
    {
        final ValidationSettings validationSettings = ValidationSettings.configure()
            .customValidators(new CustomIntPropertyValidator())
            .build();

        final ValidatorFactory validatorFactory
            = new ValidatorFactory(validationSettings, null, null, TestConfiguration.createDefault(null, null, null, null));

        final PropertyValidator validator = validatorFactory.create(null, 42, 53);
        assertTrue(validator instanceof CustomPropertyValidator,
                   "Expected a CustomPropertyValidator for unnamed int property validation despite specifying differing values when "
                   + "creating the validator, as we've configured a CustomIntPropertyValidator that ignores the name of the "
                   + "property being validated");
        assertFalse(validator.isValid(42, 53));
    }

    @Test
    public void testCustomValidatorNotUsedAsPropNameNotRecognized()
    {
        final String INT_PROP_1 = "intProp1";
        final String INT_PROP_2 = "intProp2";
        final String INT_PROP_3 = "intProp3";

        final ValidationSettings validationSettings = ValidationSettings.configure()
            .customValidators(new CustomIntPropertyValidator(INT_PROP_1, INT_PROP_2))
            .build();

        final ValidatorFactory validatorFactory
            = new ValidatorFactory(validationSettings, null, null, TestConfiguration.createDefault(null, null, null, null));

        final PropertyValidator validator = validatorFactory.create(INT_PROP_3, 42, 42);
        assertFalse(validator instanceof CustomPropertyValidator,
                    "Did not expect a CustomPropertyValidator as the configured CustomPropertyValidator does not accept the named "
                    + "property being validated");
        assertTrue(validator.isValid(42, 42));
    }

    @Test
    public void testValidationFailureForValidationTypeIncompatibleWithValidator()
    {
        final String INT_PROP_1 = "intProp1";

        final ValidationSettings validationSettings = ValidationSettings.configure()
            .customValidators(new CustomIntPropertyValidator(INT_PROP_1))
            .build();

        final ValidatorFactory validatorFactory
            = new ValidatorFactory(validationSettings, null, null, TestConfiguration.createDefault(null, null, null, null));

        final PropertyValidator validator = validatorFactory.create(INT_PROP_1, 42, 42);
        assertTrue(validator instanceof CustomPropertyValidator,
                   "Expected a CustomPropertyValidator as we've configured a CustomIntPropertyValidator that accepts the name of "
                   + "the property being validated");
        assertFalse(validator.isValid("not-an-int", "not-an-int"),
                    "Expected validation failure as the validator is testing types that it was not created to validate");
    }

    @Test
    public void testChooseFirstOfMultipleApplicableValidators()
    {
        final String INT_PROP_1 = "intProp1";
        final String INT_PROP_2 = "intProp2";
        final String INT_PROP_3 = "intProp3";

        final ValidationSettings validationSettings = ValidationSettings.configure()
            .customValidators(new CustomIntPropertyValidator(INT_PROP_1, INT_PROP_2),
                              new CustomIntPropertyValidator(INT_PROP_2, INT_PROP_3))
            .build();

        final ValidatorFactory validatorFactory
            = new ValidatorFactory(validationSettings, null, null, TestConfiguration.createDefault(null, null, null, null));

        final PropertyValidator validator = validatorFactory.create(INT_PROP_2, 42, 42);
        assertTrue(validator instanceof CustomPropertyValidator,
                   "Expected a CustomPropertyValidator as we've configured a CustomIntPropertyValidator that accepts the name of "
                   + "the property being validated");
        assertTrue(validator instanceof CustomIntPropertyValidator,
                   "Expected the validator to be specifically a CustomIntPropertyValidator");

        CustomIntPropertyValidator customIntValidator = (CustomIntPropertyValidator) validator;
        assertEquals(customIntValidator.getRecognizedPropertyNames().size(), 2,
                     "Expected to use the configured CustomIntPropertyValidator that recognizes 2 property names");
        assertTrue(customIntValidator.getRecognizedPropertyNames().contains(INT_PROP_1)
            && customIntValidator.getRecognizedPropertyNames().contains(INT_PROP_2),
                   "Expected to use the configured CustomIntPropertyValidator that recognizes the property names \""
                   + INT_PROP_1
                   + "\" and \""
                   + INT_PROP_2 + "\"");
        assertTrue(validator.isValid(42, 42));
    }

    private static class CustomIntPropertyValidator extends CustomPropertyValidator
    {
        private Collection<String> recognizedPropertyNames = new ArrayList<>();

        public CustomIntPropertyValidator(final String... recognizedPropertyNames)
        {
            this.recognizedPropertyNames = new ArrayList<>(Arrays.asList(recognizedPropertyNames));
        }

        public Collection<String> getRecognizedPropertyNames()
        {
            return recognizedPropertyNames;
        }

        @Override
        public boolean canValidate(String propertyName, Object sourcePropertyValue, Object validatorPropertyValue)
        {
            final boolean nameCheckPassed = recognizedPropertyNames.isEmpty() || recognizedPropertyNames.contains(propertyName);
            return nameCheckPassed && sourcePropertyValue instanceof Integer && validatorPropertyValue instanceof Integer;
        }

        @Override
        protected boolean isValid(Object testedPropertyValue, Object validatorPropertyValue)
        {
            return testedPropertyValue instanceof Integer
                && validatorPropertyValue instanceof Integer
                && testedPropertyValue.equals(validatorPropertyValue);
        }
    }

    private static class CustomStringPropertyValidator extends CustomPropertyValidator
    {
        @Override
        public boolean canValidate(String propertyName, Object sourcePropertyValue, Object validatorPropertyValue)
        {
            return sourcePropertyValue instanceof String
                && validatorPropertyValue instanceof String;
        }

        @Override
        protected boolean isValid(Object testedPropertyValue, Object validatorPropertyValue)
        {
            return testedPropertyValue instanceof String
                && validatorPropertyValue instanceof String
                && testedPropertyValue.equals(validatorPropertyValue);
        }
    }
}
