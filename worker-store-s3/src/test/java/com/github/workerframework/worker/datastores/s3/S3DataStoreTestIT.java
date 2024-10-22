/*
 * Copyright 2015-2024 Open Text.
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
package com.github.workerframework.worker.datastores.s3;

import com.amazonaws.util.IOUtils;
import com.github.workerframework.worker.api.DataStoreException;
import com.github.workerframework.worker.datastores.s3.S3DataStore;
import com.github.workerframework.worker.datastores.s3.S3DataStoreConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

@Test(enabled = false)
public class S3DataStoreTestIT
{
    S3DataStoreConfiguration s3DataStoreConfiguration;

    public S3DataStoreTestIT()
    {
        s3DataStoreConfiguration = new S3DataStoreConfiguration();

        s3DataStoreConfiguration.setProxyProtocol("HTTP");
        s3DataStoreConfiguration.setProxyHost("proxy.sdc.hp.com");
        s3DataStoreConfiguration.setProxyPort(8080);

        // Before running tests, the details of a valid AWS Account will need to be added below
        // *** Do Not Check In Account Details ***
        s3DataStoreConfiguration.setBucketName("username-bucket");
        s3DataStoreConfiguration.setAccessKey("Access-Key");
        s3DataStoreConfiguration.setSecretKey("Secret-Key");
    }

    /**
     * Test that a complete reference string can be processed *
     */
    @Test(enabled = false)
    public void testEverything()
        throws DataStoreException, IOException
    {
        S3DataStore s3DataStore = new S3DataStore(s3DataStoreConfiguration);
        String testString = "simple test";

        String reference = s3DataStore.store(testString.getBytes(), "/testing");

        try (InputStream inputStream = s3DataStore.retrieve(reference)) {
            String result = IOUtils.toString(inputStream);
            Assert.assertEquals(result, testString);
        }

        s3DataStore.delete(reference);

        try (InputStream inputStream = s3DataStore.retrieve(reference)) {
            String result = IOUtils.toString(inputStream);
            Assert.assertEquals(result, testString);
        } catch (DataStoreException ex) {
        }
    }
}
