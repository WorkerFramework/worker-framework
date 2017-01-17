/*
 * (c) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hpe.caf.worker.testing.configuration;

import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.TestItemStore;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;

/**
 * Created by comac on 05/05/2016.
 */
public class TestItemStoreTest {

    private final TestItemStore testItemStore = new TestItemStore(null);
    private final HashMap<String, TestItem> items = new HashMap<>();
    private final String inputIdToGet = "18d1aaab-a9c4-4698-acc2-f65a822ff718";
    private final String inputIdNotToGet = "fb414821-06b8-4686-9cea-eeb3d0904628";

    @BeforeMethod
    public void prepareTest() {
        // First TestItem to add to the Map
        TestItem testItem1 = new TestItem("ThreeFilesZip\\input\\ThreeFiles.zip", null, null);
        testItem1.setInputIdentifier("ec39fcde-2278-4200-acbc-9c2a2f5d9c72");
        testItemStore.store("ThreeFilesZip\\input\\ThreeFiles.zip", testItem1);

        // Second TestItem to add to the Map
        TestItem testItem2 = new TestItem("TwoFilesZIP\\input\\TwoFiles.zip", null, null);
        testItem2.setInputIdentifier("18d1aaab-a9c4-4698-acc2-f65a822ff718");
        testItemStore.store("TwoFilesZIP\\input\\TwoFiles.zip", testItem2);
    }

    @Test
    public void testTestItemStoreFindMethod () {
        // An Input ID that is within the Map of TestItems
        Assert.assertNotNull(testItemStore.find(inputIdToGet));
        // An Input ID that is not within the Map of TestItems
        Assert.assertNull(testItemStore.find(inputIdNotToGet));
    }
}
