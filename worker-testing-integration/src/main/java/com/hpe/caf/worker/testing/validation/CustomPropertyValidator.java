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
package com.hpe.caf.worker.testing.validation;

/**
 * Base for single-property validators that apply custom validation.
 */
public abstract class CustomPropertyValidator extends PropertyValidator
{
    /**
     * Determines whether this validator can validate properties with the specified name whose type matches that indicated by the type of
     * the sourcePropertyValue and validatorPropertyValue parameters.
     *
     * @param propertyName if not null then this method verifies whether the validator can validate properties with this property name
     * whose type matches the type of the sourcePropertyValue and validatorPropertyValue arguments. If propertyName is null then this
     * method indicates whether this validator can validate properties whose type matches the type of the sourcePropertyValue and
     * validatorPropertyValue arguments, regardless of their property name; if the validator insists on including a property name check
     * when validating then this method will return false if supplied with a null propertyName argument.
     * @param sourcePropertyValue an exemplar of the type of value to be validated
     * @param validatorPropertyValue an exemplar of the type of value against which property values will be validated
     * @return whether the validator can validate properties with the specified name (if supplied) and value types
     */
    public abstract boolean canValidate(String propertyName, Object sourcePropertyValue, Object validatorPropertyValue);
}
