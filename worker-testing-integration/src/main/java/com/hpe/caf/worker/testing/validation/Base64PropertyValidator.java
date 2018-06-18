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

import org.apache.commons.codec.binary.Base64;

import java.util.Arrays;

/**
 * The {@code Base64PropertyValidator} class. If a validated property is {@code byte[]} then the expected result will be serialized as
 * Base64 string. This validator converts Base64 string to byte array and then compares source property value (which has to be a byte
 * array) with the expected.
 */
public class Base64PropertyValidator extends PropertyValidator
{
    @Override
    protected boolean isValid(Object testedPropertyValue, Object validatorPropertyValue)
    {
        if (validatorPropertyValue == null) {
            return testedPropertyValue == null;
        }

        if (testedPropertyValue == null) {
            return false;
        }

        if (!(testedPropertyValue instanceof byte[]) || !(validatorPropertyValue instanceof String)) {
            throw new AssertionError("Unexpected types provided to Base64PropertyValidator. Expected byte array testedPropertyValue and String validatorPropertyValue. Provided were "
                + testedPropertyValue.getClass().getSimpleName() + " and " + validatorPropertyValue.getClass().getSimpleName()
                + ". Values: " + testedPropertyValue.toString() + ", " + validatorPropertyValue.toString());
        }

        byte[] testedPropertyValueBytes = (byte[]) testedPropertyValue;

        if (validatorPropertyValue.toString().equals("*")) {
            return testedPropertyValueBytes.length > 0;
        }

        boolean areEqual = Arrays.equals(testedPropertyValueBytes, Base64.decodeBase64(validatorPropertyValue.toString()));

        if (!areEqual) {
            String actual = Base64.encodeBase64String(testedPropertyValueBytes);
            System.err.println("Unexpected result. Actual value: " + actual + ", expected value: " + validatorPropertyValue.toString());
        }

        return areEqual;
    }
}
