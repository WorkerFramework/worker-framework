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
package com.hpe.caf.worker.testing.configuration;

/**
 * Created by ploch on 04/12/2015.
 */
public class TestCaseClasses<TInput, TExpectation>
{
    private Class<TInput> inputClass;

    private Class<TExpectation> expectationClass;

    public TestCaseClasses(Class<TInput> inputClass, Class<TExpectation> expectationClass)
    {
        this.inputClass = inputClass;
        this.expectationClass = expectationClass;
    }

    /**
     * Getter for property 'inputClass'.
     *
     * @return Value for property 'inputClass'.
     */
    public Class<TInput> getInputClass()
    {
        return inputClass;
    }

    /**
     * Getter for property 'expectationClass'.
     *
     * @return Value for property 'expectationClass'.
     */
    public Class<TExpectation> getExpectationClass()
    {
        return expectationClass;
    }
}
