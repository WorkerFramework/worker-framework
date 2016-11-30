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
