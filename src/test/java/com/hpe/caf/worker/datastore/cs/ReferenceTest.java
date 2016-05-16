package com.hpe.caf.worker.datastore.cs;

import org.apache.http.NameValuePair;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ReferenceTest {

    @Test
    public void testReferenceWithContainerId()
    {
        String testReference = "3e7a3c99b9a1486d9c20abe236cd1909";

        Reference ref = new Reference(testReference);
        ReferenceComponents refComponents = ref.parse();

        Assert.assertEquals(refComponents.getContainerId(), testReference);
        Assert.assertNull(refComponents.getAssetId());
        Assert.assertNull(refComponents.getNameValueCollection());
    }

    @Test
    public void testReferenceWithContainerAndAssetIds()
    {
        String testContainerId = "c82335049236404ba86529e9afacba39";
        String testAssetId = "cd423dc709ba495b854a10cd612f2ff1";
        String testReference = testContainerId + "/" + testAssetId;

        Reference ref = new Reference(testReference);
        ReferenceComponents refComponents = ref.parse();

        Assert.assertEquals(refComponents.getContainerId(), testContainerId);
        Assert.assertEquals(refComponents.getAssetId(), testAssetId);
        Assert.assertNull(refComponents.getNameValueCollection());
    }

    @Test
    public void testReferenceWithContainerAndDelegationTicket()
    {
        String testContainerId = "c82335049236404ba86529e9afacba39";
        String testDelegationTicket = "?delegationTicket=test%20ticket";
        String testReference = testContainerId + testDelegationTicket;

        Reference ref = new Reference(testReference);
        ReferenceComponents refComponents = ref.parse();

        Assert.assertEquals(refComponents.getContainerId(), testContainerId);
        Assert.assertNull(refComponents.getAssetId());
        Assert.assertNotNull(refComponents.getNameValueCollection());

        String delegationTicket="";
        List<NameValuePair> params = refComponents.getNameValueCollection();
        if (params != null && !params.isEmpty()) {
            for (NameValuePair nvp : params) {
                if (nvp.getName().equals("delegationTicket")) {
                    delegationTicket = nvp.getValue();
                    break;
                }
            }
        }
        Assert.assertEquals("test ticket", delegationTicket);
    }

    @Test
    public void testReferenceWithContainerAssetAndDelegationTicket()
    {
        String testContainerId = "c82335049236404ba86529e9afacba39";
        String testAssetId = "cd423dc709ba495b854a10cd612f2ff1";
        String testDelegationTicket = "?delegationTicket=test%20ticket";
        String testReference = testContainerId + "/" + testAssetId + testDelegationTicket;

        Reference ref = new Reference(testReference);
        ReferenceComponents refComponents = ref.parse();

        Assert.assertEquals(refComponents.getContainerId(), testContainerId);
        Assert.assertEquals(refComponents.getAssetId(), testAssetId);
        Assert.assertNotNull(refComponents.getNameValueCollection());

        String delegationTicket="";
        List<NameValuePair> params = refComponents.getNameValueCollection();
        if (params != null && !params.isEmpty()) {
            for (NameValuePair nvp : params) {
                if (nvp.getName().equals("delegationTicket")) {
                    delegationTicket = nvp.getValue();
                    break;
                }
            }
        }
        Assert.assertEquals("test ticket", delegationTicket);
    }

}
