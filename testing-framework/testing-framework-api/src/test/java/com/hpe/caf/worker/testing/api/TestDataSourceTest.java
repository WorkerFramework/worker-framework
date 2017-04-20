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

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by ploch on 14/04/2017.
 */
public class TestDataSourceTest
{
    @Test
    public void testGetDataByClass() throws Exception
    {
        TestDataSource sut = new TestDataSource();
        sut.addData("my-data");
        sut.addData(Paths.get("."));

        String stringData = sut.getData(String.class);

        Path pathData = sut.getData(Path.class);

        assertThat(stringData, is("my-data"));
        assertThat(pathData.toAbsolutePath(), is(Paths.get(".").toAbsolutePath()));


    }
}
