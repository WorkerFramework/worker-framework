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

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.worker.testing.TestResultHelper;

import java.util.*;

/**
 * Created by gibsodom on 07/01/2016.
 */
public class UnorderedArrayReferencedDataValidator extends PropertyValidator
{
    private DataStore dataStore;
    private Codec codec;
    private String testDataLocation;
    private String testSourcefileBaseFolder;

    public UnorderedArrayReferencedDataValidator(DataStore store, Codec codec, String testDataLocation, String testSourcefileBaseFolder)
    {
        this.dataStore = store;
        this.codec = codec;
        this.testDataLocation = testDataLocation;
        this.testSourcefileBaseFolder = testSourcefileBaseFolder;
    }

    @Override
    protected boolean isValid(Object testedPropertyValue, Object validatorPropertyValue)
    {
        if (testedPropertyValue == null && validatorPropertyValue == null) {
            return true;
        }
        if (!(testedPropertyValue instanceof Collection)) {
            return false;
        }
        if (!(validatorPropertyValue instanceof Collection)) {
            return false;
        }
        Object[] testedArray = ((Collection) testedPropertyValue).toArray();

        // For each element in the testedArray, compare it with each element in the validationArray by using the ReferenceDataValidator.
        // Remove validationArray elements that match with testedArray elements.
        for (Object testedValue : testedArray) {
            Iterator<Object> validationArrayIterator = ((Collection) validatorPropertyValue).iterator();
            PropertyValidator validator = new ReferenceDataValidator(false, dataStore, codec, testDataLocation, testSourcefileBaseFolder);
            // If the testedValue contains objects that we can iterate over, iterate over them and check their validity
            if (testedValue instanceof LinkedHashMap) {
                HashMap fieldsToTest = (LinkedHashMap<Object, Object>) testedValue;
                for (Object fieldToTest : fieldsToTest.values()) {
                    boolean valid = checkValidity(validator, fieldToTest, validationArrayIterator);
                    if (!valid) {
                        return false;
                    }
                }
            } else {
                boolean valid = checkValidity(validator, testedValue, validationArrayIterator);
                if (!valid) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Using a PropertyValidator validate a value for testing against an Iterator containing Objects
     *
     * @param validator PropertyValidator to use for validation
     * @param testedValue an Object to test validity of
     * @param validationArrayIterator an Iterator of Objects to for testedValue to validate against
     * @return boolean on success or failure of validating testedValue object against a validationValue
     */
    public boolean checkValidity(PropertyValidator validator, Object testedValue, Iterator<Object> validationArrayIterator)
    {
        boolean valid = false;
        while (validationArrayIterator.hasNext()) {
            Object validationValue = validationArrayIterator.next();
            valid = validator.isValid(testedValue, validationValue);
            // If the validation comparison was not valid and there are no more elements to validate against, return false
            if (!valid && !validationArrayIterator.hasNext()) {
                TestResultHelper.testFailed("Unsuccessfully validated " + testedValue.toString() + " against "
                    + validationValue.toString() + ". No more elements to validate against.");
                return false;
            } else if (valid) {
                System.out.println(testedValue.toString() + " validated successfully against " + validationValue.toString());
                validationArrayIterator.remove();
                return true;
            } else {
                System.err.println(testedValue.toString() + " unsuccessfully validated against " + validationValue.toString()
                    + ". Attempting to validate against next element.");
            }
        }
        return valid;
    }
}
