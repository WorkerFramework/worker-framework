package com.hpe.caf.util.ref;


public class DummyMessage
{
    private ReferencedObject<String> dummyObject;


    public DummyMessage() { }


    public ReferencedObject<String> getDummyObject()
    {
        return dummyObject;
    }


    public void setDummyObject(final ReferencedObject<String> dummyObject)
    {
        this.dummyObject = dummyObject;
    }
}
