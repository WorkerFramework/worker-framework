package com.hpe.caf.worker.testing.configuration;

import com.hpe.caf.worker.testing.validation.PropertyMap;
import com.hpe.caf.worker.testing.validation.PropertyMapValidator;
import com.hpe.caf.worker.testing.validation.ValidatorFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Christopher Comac on 15/09/2016.
 */
public class PropertyMapValidatorPropertyMismatchTest {

    /* This tests that the PropertyMapValidator throws an AssertionError if there is a property within the expected
        set of properties that is not returned within the set of actual properties.
     */
    @Test
    public void testMismatchOfPropertiesWithinActualPropertyMap() {

        PropertyMap mapOfExpectedProps = new PropertyMap();
        mapOfExpectedProps.put("knownField1", "knownValue1");
        mapOfExpectedProps.put("knownField2", "knownValue2");
        mapOfExpectedProps.put("knownField3", "knownValue3");

        PropertyMap mapOfActualProps = new PropertyMap();
        mapOfActualProps.put("knownField1", "knownValue1");
        mapOfActualProps.put("knownField2", "knownValue2");

        PropertyMapValidator propertyMapValidator = new PropertyMapValidator(new ValidatorFactory(
                ValidationSettings.configure().build(), null, null, null));

        boolean propertyMismatch = false;
        try {
            propertyMapValidator.process(mapOfActualProps, mapOfExpectedProps);
        } catch (AssertionError error) {
            String expectedError = "Actual result item under test doesn't have knownField3 property belonging to the" +
                    " expected filter data validation file.";
            Assert.assertEquals("Should throw AssertionError on expected to actual property mismatch",
                    expectedError, error.getMessage());
            propertyMismatch = true;
        }

        // Fail if there were no missing properties in the actual properties
        if (!propertyMismatch) Assert.fail("No missing properties within map of actual properties");
    }

    /* This tests that the PropertyMapValidator throws an AssertionError if there is a new property within the actual
        set of properties returned that is not within the set of expected properties..
     */
    @Test
    public void testMismatchOfPropertiesWithinExpectedPropertyMap() {

        PropertyMap mapOfExpectedProps = new PropertyMap();
        mapOfExpectedProps.put("knownField1", "knownValue1");

        PropertyMap mapOfActualProps = new PropertyMap();
        mapOfActualProps.put("knownField1", "knownValue1");
        mapOfActualProps.put("unknownField2", "unknownValue2");

        PropertyMapValidator propertyMapValidator = new PropertyMapValidator(new ValidatorFactory(
                ValidationSettings.configure().build(), null, null, null));

        boolean propertyMismatch = false;
        try {
            propertyMapValidator.process(mapOfActualProps, mapOfExpectedProps);
        } catch (AssertionError error) {
            String expectedError = "Expected filter data validation file used doesn't have unknownField2 property " +
                    "belonging to the actual result.";
            Assert.assertEquals("Should throw AssertionError on actual to expected property mismatch",
                    expectedError, error.getMessage());
            propertyMismatch = true;
        }

        // Fail if there were no missing properties in the expected properties
        if (!propertyMismatch) Assert.fail("No missing properties within map of expected properties");
    }
}
