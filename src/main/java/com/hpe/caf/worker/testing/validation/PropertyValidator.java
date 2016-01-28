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
    public void validate(String propertyName, Object testedPropertyValue, Object validatorPropertyValue){
        boolean isValid = isValid(testedPropertyValue, validatorPropertyValue);

        if (!isValid) {
            throw new AssertionError(String.format("Validator name: %s.\nValidation of property '%s' failed.\n Actual property value: %s\n Validation string: %s", getClass().getSimpleName(), propertyName, testedPropertyValue, validatorPropertyValue));
        }
    }
}
