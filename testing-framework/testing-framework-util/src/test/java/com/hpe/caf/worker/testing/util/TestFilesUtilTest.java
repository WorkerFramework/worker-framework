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
package com.hpe.caf.worker.testing.util;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by ploch on 18/04/2017.
 */
public class TestFilesUtilTest
{
    @Test
    public void testGetTestDataRootPathWhenEnvironmentVariableIsSet() throws Exception
    {
        System.setProperty(TestingConstants.EnvironmentVariables.TEST_DATA_ROOT, "src\\test\\test-data\\folder-1");

        Path testDataRootPath = TestFilesUtil.getTestDataRootPath();

        assertThat(testDataRootPath.toString(), is("src\\test\\test-data\\folder-1"));
        assertThat(Files.exists(testDataRootPath), is(true));
    }

    @Test
    public void testGetTestDataRootPathWhenEnvironmentVariableIsNotSet() throws Exception
    {
        Path testDataRootPath = TestFilesUtil.getTestDataRootPath();
        assertThat(testDataRootPath.toString(), is("src\\test\\test-data"));
        assertThat(Files.exists(testDataRootPath), is(true));
    }
}
