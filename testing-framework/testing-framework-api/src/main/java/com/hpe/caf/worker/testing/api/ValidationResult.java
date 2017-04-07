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
package com.hpe.caf.worker.testing.api;

import java.util.Map;
import java.util.Set;

/**
 * Created by ploch on 05/11/2016.
 */
public class ValidationResult {

    private ValidationStatus status;
    private Object actualObject;
    private Object expectedObject;
    private String message;
    private Map<String, Object> additionalData;

    private String validatorName;

    private Set<ValidationResult> validationResults;

    public ValidationStatus getStatus() {
        return status;
    }

    public Object getActualObject() {
        return actualObject;
    }

    public Object getExpectedObject() {
        return expectedObject;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public String getValidatorName() {
        return validatorName;
    }

    public Set<ValidationResult> getValidationResults() {
        return validationResults;
    }
}
