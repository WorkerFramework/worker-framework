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
package com.hpe.caf.worker.testing;

import com.hpe.caf.worker.testing.api.TestCaseInfo;
import com.hpe.caf.worker.testing.preparation.DefaultPathTestCaseInfoFactory;
import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import java.nio.file.Paths;

/**
 * Created by ploch on 17/03/2017.
 */
public class DefaultPathTestCaseInfoFactoryTest {
    @Test
    public void testCreateUsingFileName() throws Exception {

        DefaultPathTestCaseInfoFactory sut = new DefaultPathTestCaseInfoFactory("C:\\test-data");

        TestCaseInfo info = sut.create(Paths.get("C:\\test-data", "my-test-file.txt"));

        assertThat(info.getTestCaseId(), is("my-test-file.txt"));
    }

    @Test
    public void testCreateUsingParentFolderName() throws Exception {

        DefaultPathTestCaseInfoFactory sut = new DefaultPathTestCaseInfoFactory("C:\\test-data");

        TestCaseInfo info = sut.create(Paths.get("C:\\test-data", "test-case-1", "my-test-file.txt"));

        assertThat(info.getTestCaseId(), is("test-case-1"));
    }
}
