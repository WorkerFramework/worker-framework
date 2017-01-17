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

package com.hpe.caf.worker.datastore.cs;


import com.hpe.caf.api.worker.DataStoreException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.UUID;


public class CafStoreReferenceTest
{
    /** Test that a complete reference string can be processed **/
    @Test
    public void testReferenceFromString()
        throws DataStoreException
    {
        String container = getNewUUIDString();
        String asset = getNewUUIDString();
        CafStoreReference test = new CafStoreReference(container + "/" + asset);
        Assert.assertEquals(container, test.getContainer());
        Assert.assertEquals(container, test.getIndex(0));
        Assert.assertEquals(asset, test.getAsset());
        Assert.assertEquals(asset, test.getIndex(1));
    }


    /** Test that a complete reference can be generated from a separate container and asset **/
    @Test
    public void testReferenceFromComponents()
        throws DataStoreException
    {
        String container = getNewUUIDString();
        String asset = getNewUUIDString();
        CafStoreReference test = new CafStoreReference(container, asset);
        Assert.assertEquals(container, test.getContainer());
        Assert.assertEquals(container, test.getIndex(0));
        Assert.assertEquals(asset, test.getAsset());
        Assert.assertEquals(asset, test.getIndex(1));
    }


    /** Check that a non-UUID container is invalid **/
    @Test(expectedExceptions = DataStoreException.class)
    public void testInvalidContainer()
        throws DataStoreException
    {
        String container = "01234:45678";
        String asset = getNewUUIDString();
        CafStoreReference test = new CafStoreReference(container, asset);
    }


    /** Check that a non-UUID asset is invalid **/
    @Test(expectedExceptions = DataStoreException.class)
    public void testInvalidAsset()
        throws DataStoreException
    {
        String container = getNewUUIDString();
        String asset = "01234:45678";
        CafStoreReference test = new CafStoreReference(container, asset);
    }


    /** Test that a reference is not generated from too many components **/
    @Test(expectedExceptions = DataStoreException.class)
    public void testInvalidReference()
        throws DataStoreException
    {
        String container = getNewUUIDString();
        String asset = getNewUUIDString();
        CafStoreReference test = new CafStoreReference(container + "/" + asset + "/01234");
    }


    /** Test that a reference is not generated when components are missing **/
    @Test(expectedExceptions = DataStoreException.class)
    public void testMissingReference()
        throws DataStoreException
    {
        String container = getNewUUIDString();
        CafStoreReference test = new CafStoreReference(container);
    }

    private String getNewUUIDString(){
        return UUID.randomUUID().toString().replace("-","");
    }

}
