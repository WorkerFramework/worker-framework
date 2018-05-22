/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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

import java.util.Collection;

/**
 * Created by ploch on 08/12/2015.
 */
public class CollectionValidator extends PropertyValidator
{
    private final ValidatorFactory validatorFactory;

    public CollectionValidator(ValidatorFactory validatorFactory)
    {
        this.validatorFactory = validatorFactory;
    }

    @Override
    protected boolean isValid(Object testedPropertyValue, Object validatorPropertyValue)
    {
        if (!(testedPropertyValue instanceof Collection)) {
            return false;
        }
        if (!(validatorPropertyValue instanceof Collection)) {
            return false;
        }

        Object[] testedArray = ((Collection) testedPropertyValue).toArray();

        Object[] validationArray = ((Collection) validatorPropertyValue).toArray();

        for (int i = 0; i < validationArray.length; i++) {
            Object testedValue = testedArray[i];
            Object validationValue = validationArray[i];
            PropertyValidator validator = validatorFactory.create(null, testedValue, validationValue);
            validator.validate("collection-entry", testedValue, validationValue);
        }

        return true;
    }
}
