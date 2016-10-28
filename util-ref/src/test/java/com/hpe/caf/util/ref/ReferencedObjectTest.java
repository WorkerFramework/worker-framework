package com.hpe.caf.util.ref;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;


public class ReferencedObjectTest
{
    @Test
    public void testAcquireWithObject()
        throws DataSourceException
    {
        String test = "test123";
        ReferencedObject<String> testRef = ReferencedObject.getWrappedObject(String.class, test);
        DataSource source = Mockito.mock(DataSource.class);
        Assert.assertEquals(test, testRef.acquire(source));
        Mockito.verify(source, Mockito.times(0)).getObject(Mockito.any(), Mockito.any());
        Assert.assertNull(testRef.getReference());
    }


    @Test
    public void testAcquireWithReference()
        throws DataSourceException
    {
        String test = "test123";
        String ref = "ref123";
        ReferencedObject<String> testRef = ReferencedObject.getReferencedObject(String.class, ref);
        DataSource source = Mockito.mock(DataSource.class);
        Assert.assertNotNull(testRef.getReference());
        Mockito.when(source.getObject(ref, String.class)).thenReturn(test);
        Assert.assertEquals(test, testRef.acquire(source));
    }


    @Test(expected = IllegalStateException.class)
    public void testMissingObjectAndReference()
        throws DataSourceException
    {
        ReferencedObject<String> test = new ReferencedObject<>();
        test.acquire(Mockito.mock(DataSource.class));
    }


    @Test
    public void testSerialisation()
        throws DataSourceException, IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        String test = "test123";
        ReferencedObject<String> testRef = ReferencedObject.getWrappedObject(String.class, test);
        DummyMessage dummy = new DummyMessage();
        dummy.setDummyObject(testRef);
        byte[] ser = mapper.writeValueAsBytes(dummy);
        DummyMessage res = mapper.readValue(ser, DummyMessage.class);
        DataSource source = Mockito.mock(DataSource.class);
        Assert.assertEquals(test, res.getDummyObject().acquire(source));
    }
}
