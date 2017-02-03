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

/**
 * Base class for element level (single property or name=value pair) validators.
 * Worker result is converted to a {@link java.util.Map} of {@code String} and {@code Object}.
 * Each property of the result will be the map entry with a name and value of the property.
 * Expectation is read as a Map as well and then each entry of worker result and expectation
 * is passed to implementations of this class.
 */
public abstract class PropertyValidator {

    /**
     * The isValid method does the actual comparison.
     * Actual comparison logic for a validator.
     *
     * @param testedPropertyValue    the tested property value
     * @param validatorPropertyValue the validator property value
     * @return the boolean
     */
    protected abstract boolean isValid(Object testedPropertyValue, Object validatorPropertyValue);

    /**
     * Method {@code validate} is the entry point for the root validator.
     * Entry point for property (map entry) validation.
     * This method will throw {@link AssertionError} if validation fails.
     *
     * @param propertyName           the property name
     * @param testedPropertyValue    the tested property value
     * @param validatorPropertyValue the expected property value
     */
    public final void validate(String propertyName, Object testedPropertyValue, Object validatorPropertyValue){
        boolean isValid = isValid(testedPropertyValue, validatorPropertyValue);

        if (!isValid) {
            throw new AssertionError(String.format("Validator name: %s.\nValidation of property '%s' failed.\n Actual property value: %s\n Validation string: %s", getClass().getSimpleName(), propertyName, testedPropertyValue, validatorPropertyValue));
        }
    }
}
