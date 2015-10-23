package com.hpe.caf.worker.datastore.cs;


import com.hpe.caf.api.worker.DataStoreException;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;


public class CafStoreReferenceTest
{
    /** Test that a complete reference string can be processed **/
    @Test
    public void testReferenceFromString()
        throws DataStoreException
    {
        String container = UUID.randomUUID().toString();
        String asset = UUID.randomUUID().toString();
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
        String container = UUID.randomUUID().toString();
        String asset = UUID.randomUUID().toString();
        CafStoreReference test = new CafStoreReference(container, asset);
        Assert.assertEquals(container, test.getContainer());
        Assert.assertEquals(container, test.getIndex(0));
        Assert.assertEquals(asset, test.getAsset());
        Assert.assertEquals(asset, test.getIndex(1));
    }


    /** Check that a non-UUID container is invalid **/
    @Test(expected = DataStoreException.class)
    public void testInvalidContainer()
        throws DataStoreException
    {
        String container = "01234:45678";
        String asset = UUID.randomUUID().toString();
        CafStoreReference test = new CafStoreReference(container, asset);
    }


    /** Check that a non-UUID asset is invalid **/
    @Test(expected = DataStoreException.class)
    public void testInvalidAsset()
        throws DataStoreException
    {
        String container = UUID.randomUUID().toString();
        String asset = "01234:45678";
        CafStoreReference test = new CafStoreReference(container, asset);
    }


    /** Test that a reference is not generated from too many components **/
    @Test(expected = DataStoreException.class)
    public void testInvalidReference()
        throws DataStoreException
    {
        String container = UUID.randomUUID().toString();
        String asset = UUID.randomUUID().toString();
        CafStoreReference test = new CafStoreReference(container + "/" + asset + "/01234");
    }


    /** Test that a reference is not generated when components are missing **/
    @Test(expected = DataStoreException.class)
    public void testMissingReference()
        throws DataStoreException
    {
        String container = UUID.randomUUID().toString();
        CafStoreReference test = new CafStoreReference(container);
    }

}
