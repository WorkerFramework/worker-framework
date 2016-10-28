package com.hpe.caf.worker.datastore.cs;

import org.apache.http.NameValuePair;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ReferenceComponentsTest {

    private final String DELEGATION_TICKET_NAMED_PARAM = "delegationTicket";

    @Test
    public void testReferenceWithNoDelegationTicket()
    {
        String testReference ="c82335049236404ba86529e9afacba39/cd423dc709ba495b854a10cd612f2ff1";

        ReferenceComponents refComponents = ReferenceComponents.parseReference(testReference);

        Assert.assertEquals(refComponents.getReference(), testReference);
        Assert.assertNull(refComponents.getNamedValue(DELEGATION_TICKET_NAMED_PARAM));
    }


    @Test
    public void testReferenceWithDelegationTicket()
    {
        String testContainerAssetIds ="c82335049236404ba86529e9afacba39/cd423dc709ba495b854a10cd612f2ff1";
        String testDelegationTicket = "?delegationTicket=test%20ticket";
        String testReference = testContainerAssetIds + testDelegationTicket;

        ReferenceComponents refComponents = ReferenceComponents.parseReference(testReference);

        Assert.assertEquals(refComponents.getReference(), testContainerAssetIds);
        Assert.assertEquals("test ticket", refComponents.getNamedValue(DELEGATION_TICKET_NAMED_PARAM));
    }
}
