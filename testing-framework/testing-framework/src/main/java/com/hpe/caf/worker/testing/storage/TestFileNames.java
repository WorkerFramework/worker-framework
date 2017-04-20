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
package com.hpe.caf.worker.testing.storage;

/**
 * Created by ploch on 20/04/2017.
 */
public class TestFileNames
{
    public static final String TEST_PREFIX_EXTENSION = "test";
    public static final String DESCRIPTOR_SUFFIX_EXTENTION = "descriptor";
    public static final String DESCRIPTOR_EXTENSION = TEST_PREFIX_EXTENSION + "." + DESCRIPTOR_SUFFIX_EXTENTION;  // "test.descriptor";

    public static final String EXPECTATION_SUFFIX_EXTENSION = "expectation";
    public static final String EXPECTATION_EXTENSION =  TEST_PREFIX_EXTENSION + "." + EXPECTATION_SUFFIX_EXTENSION; //"test.expectation";


    public static final String TEST_FILE_GLOB_FILTER = "*.{" + DESCRIPTOR_EXTENSION + "," + EXPECTATION_EXTENSION + "}";
    public static final String TEST_EXPECTATION_GLOB_FILTER = "*.{" + DESCRIPTOR_EXTENSION + "," + EXPECTATION_SUFFIX_EXTENSION + "}";
    public static final String TEST_DESCRIPTOR_GLOB_FILTER = "*.{" + DESCRIPTOR_EXTENSION + "," + DESCRIPTOR_SUFFIX_EXTENTION+ "}";

}
