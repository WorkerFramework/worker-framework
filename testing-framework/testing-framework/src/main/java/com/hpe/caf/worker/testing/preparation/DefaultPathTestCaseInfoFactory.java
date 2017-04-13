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
package com.hpe.caf.worker.testing.preparation;

import com.hpe.caf.worker.testing.api.TestCaseInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by ploch on 17/03/2017.
 */
public class DefaultPathTestCaseInfoFactory implements TestCaseInfoFactory
{

    private final String rootPath;

    public DefaultPathTestCaseInfoFactory(String rootPath)
    {

        this.rootPath = rootPath;
    }

    @Override
    public TestCaseInfo create(Path file)
    {

        if (Files.isDirectory(file)) {
            throw new IllegalArgumentException("Directory provided but only file is accepted.");
        }

        String rootPath = Paths.get(this.rootPath).toAbsolutePath().toString();
        Path fileParentPath = file.getParent();
        String fileParentPathString = fileParentPath.toAbsolutePath().toString();

        String testCaseId;
        if (fileParentPathString.equalsIgnoreCase(rootPath)) {
            testCaseId = file.getFileName().toString();
        }
        else {
            testCaseId = fileParentPath.getFileName().toString();
        }

        TestCaseInfo info = new TestCaseInfo(testCaseId, null, testCaseId, testCaseId);
        return info;
    }
}
