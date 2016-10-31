package com.hpe.caf.worker.testing.configuration;

import com.hpe.caf.worker.testing.SettingNames;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.validation.PropertyMap;
import com.hpe.caf.worker.testing.validation.PropertyMapValidator;
import com.hpe.caf.worker.testing.validation.ValidatorFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Christopher Comac on 15/09/2016.
 */
public class PropertyMapValidatorPropertyMismatchTest {

    /* This tests that the PropertyMapValidator throws an AssertionError if there is a new property within the actual
        set of properties returned that is not within the set of expected properties..
     */
    @Test
    public void testNewPropertiesWithinActualOutputPropertyMap() {

        PropertyMap mapOfExpectedProps = new PropertyMap();
        mapOfExpectedProps.put("knownField1", "knownValue1");

        PropertyMap mapOfActualProps = new PropertyMap();
        mapOfActualProps.put("knownField1", "knownValue1");
        mapOfActualProps.put("unknownField2", "unknownValue2");
        mapOfActualProps.put("unknownField3", "unknownValue3");

        System.setProperty(SettingNames.failOnUnknownProperty, "true");

        PropertyMapValidator propertyMapValidator = new PropertyMapValidator(new ValidatorFactory(
                ValidationSettings.configure().build(), null, null, TestConfiguration.createDefault(null, null, null,
                null)));

        boolean propertyMismatch = false;
        try {
            propertyMapValidator.process(mapOfActualProps, mapOfExpectedProps);
        } catch (AssertionError error) {
            Assert.assertTrue("Should throw AssertionError on actual to expected property mismatch", error.getMessage().contains("unknownField3"));
            propertyMismatch = true;
        }

        // Fail if there were no additional properties in the actual set of properties
        if (!propertyMismatch) Assert.fail("No additional properties found within the list of actuals when compared " +
                "with the expected");
    }
}
