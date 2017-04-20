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

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by ploch on 17/03/2017.
 */
public class TestFilesUtil
{
    private static final Logger LOG = LoggerFactory.getLogger(TestFilesUtil.class);

    public static Path getTestDataRootPath()
    {
        String testDataRootSetting = SettingsProvider.defaultProvider.getSetting(TestingConstants.EnvironmentVariables.TEST_DATA_ROOT);

        Path testDataRootPath = Strings.isNullOrEmpty(testDataRootSetting) ?
                Paths.get(".", "src", "test", "test-data") : Paths.get(testDataRootSetting);

        testDataRootPath = testDataRootPath.normalize();

        LOG.debug("Resolved TestDataRootPath: {} (absolute: {})", testDataRootPath, testDataRootPath.toAbsolutePath().toString());
        return testDataRootPath.normalize();
    }

    public static Path getTestDataPath(String... subFolders)
    {
        return Paths.get(getTestDataRootPathString(), subFolders);
    }

    public static String getTestDataRootPathString()
    {
        return getTestDataRootPath().toString();
    }
}
