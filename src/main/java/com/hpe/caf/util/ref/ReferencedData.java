package com.hpe.caf.util.ref;


import java.io.ByteArrayInputStream;
import java.io.InputStream;


/**
 * Utility wrapper for allowing data to potentially be within a message or located on a remote DataStore.
 * The acquire(ObjectSource) method allows transparent method of obtaining an InputStream to the data.
 *
 * This is primarily intended for use with large amounts of binary data that needs to be processed without
 * storing it all in memory.
 * @since 1.0
 */
public class ReferencedData
{
    private String reference;
    private byte[] data;


    // for deserialisation
    ReferencedData() { }


    /**
     * Create a new ReferencedData object.
     * @param ref the optional reference to be interpreted which points to the object
     * @param data the optional actual data itself
     */
    ReferencedData(final String ref, final byte[] data)
    {
        this.reference = ref;
        this.data = data;
    }


    /**
     * @return the remote reference to the object in the DataStore, if set
     */
    public String getReference()
    {
        return reference;
    }


    /**
     * Return the referenced data as a stream, potentially performing a remote lookup.
     * @param source the implementation that provides object instances given the provided references
     * @return a stream of the data that this container is wrapping
     * @throws DataSourceException if the data cannot be acquired
     * @throws IllegalStateException if there is no object or reference present
     */
    public synchronized InputStream acquire(final DataSource source)
        throws DataSourceException
    {
        InputStream ret;
        if ( data == null ) {
            if ( getReference() == null ) {
                throw new IllegalStateException("No data or reference present");
            } else {
                ret = source.getStream(getReference());
            }
        } else {
            ret = new ByteArrayInputStream(data);
        }
        return ret;
    }


    /**
     * Create a ReferencedData object that uses a remote reference to data present in an ObjectSource.
     * @param ref the reference to be interpreted by the DataStore
     * @return a new ReferencedData instance that relates to data in an ObjectSource via reference
     */
    public static ReferencedData getReferencedData(final String ref)
    {
        return new ReferencedData(ref, null);
    }


    /**
     * Create a ReferencedData instance that directly wraps data without a reference.
     * @param data the raw data to wrapper
     * @return a new ReferencedData instance that directly wraps the data specified
     */
    public static ReferencedData getWrappedData(final byte[] data)
    {
        return new ReferencedData(null, data);
    }
}
