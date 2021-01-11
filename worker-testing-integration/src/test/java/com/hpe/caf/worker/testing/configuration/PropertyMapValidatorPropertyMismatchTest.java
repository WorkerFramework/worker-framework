/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.testing.configuration;

import com.hpe.caf.worker.testing.SettingNames;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.validation.PropertyMap;
import com.hpe.caf.worker.testing.validation.PropertyMapValidator;
import com.hpe.caf.worker.testing.validation.ValidatorFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PropertyMapValidatorPropertyMismatchTest
{
    /* This tests that the PropertyMapValidator throws an AssertionError if there is a new property within the actual
        set of properties returned that is not within the set of expected properties..
     */
    @Test
    public void testNewPropertiesWithinActualOutputPropertyMap()
    {
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
            Assert.assertTrue(error.getMessage().contains("unknownField3"), "Should throw AssertionError on actual to expected property mismatch");
            propertyMismatch = true;
        }

        // Fail if there were no additional properties in the actual set of properties
        if (!propertyMismatch) {
            Assert.fail("No additional properties found within the list of actuals when compared "
                + "with the expected");
        }
    }
}
