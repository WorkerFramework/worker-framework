package com.hpe.caf.util.ref;


import java.util.Objects;


/**
 * Utility wrapper for allowing data to potentially be within a message or located on a remote DataStore.
 * The acquire(ObjectSource) method allows transparent method of obtaining the wrapped data, which will
 * only be retrieved the first time acquire is called (if it is not already present).
 *
 * It should be noted that because this class returns a fully formed Java object instance, the entire
 * instance must be loaded into memory. As such, this wrapper should only be used for objects that are at
 * most a few megabytes of data.
 * @param <T> the type of the wrapped, referenced object
 * @since 1.0
 */
public final class ReferencedObject<T>
{
    private T object;
    private String reference;
    private Class<T> objectClass;


    // for deserialisation
    ReferencedObject() { }


    /**
     * Create a new Referenced object.
     * @param objectClass the class of the object being referenced
     * @param ref the optional reference to be interpreted which points to the object
     * @param obj the optional actual object itself
     */
    ReferencedObject(final Class<T> objectClass, final String ref, final T obj)
    {
        this.objectClass = Objects.requireNonNull(objectClass);
        this.reference = ref;
        this.object = obj;
    }


    /**
     * Return the referenced object, potentially performing a remote DataStore lookup and deserialisation.
     * If the object is already present or has been previously acquired, it is immediately returned.
     * @param source the implementation that provides object instances given the provided references
     * @return the object that this container is wrapping
     * @throws DataSourceException if the object cannot be acquired
     * @throws IllegalStateException if there is no object or reference present
     */
    public synchronized T acquire(final DataSource source)
        throws DataSourceException
    {
        if ( object == null ) {
            if ( getReference() == null ) {
                throw new IllegalStateException("No reference or object present");
            } else {
                object = source.getObject(getReference(), objectClass);
            }
        }
        return object;
    }


    /**
     * @return the remote reference to the object in the DataStore, if set
     */
    public String getReference()
    {
        return reference;
    }


    /**
     * Create a ReferencedObject that uses a remote reference to data present in an ObjectSource.
     * @param clazz the class of the referenced object
     * @param ref the reference to be interpreted by the DataStore
     * @param <T> the type of the referenced object
     * @return a new ReferencedObject instance that relates to a remote object via reference
     */
    public static <T> ReferencedObject<T> getReferencedObject(final Class<T> clazz, final String ref)
    {
        return new ReferencedObject<>(clazz, ref, null);
    }


    /**
     * Create a ReferencedObject that directly wraps an object without a reference.
     * @param clazz the class of the object
     * @param obj the object to wrapper
     * @param <T> the type of the object
     * @return a new ReferencedObject instance that directly wraps the object specified
     */
    public static <T> ReferencedObject<T> getWrappedObject(final Class<T> clazz, final T obj)
    {
        return new ReferencedObject<>(clazz, null, obj);
    }
}
