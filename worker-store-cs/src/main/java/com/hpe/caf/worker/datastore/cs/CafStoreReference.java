package com.hpe.caf.worker.datastore.cs;


import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.naming.Name;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A CafStoreReference is like a standard Name but most only have two components, and both
 * components must be a valid UUID.
 */
public class CafStoreReference extends Name
{
    final Pattern pattern = Pattern.compile("^[0-9a-f]{32}$");

    public CafStoreReference(String completeReference)
        throws DataStoreException
    {
        super(completeReference);
        validate();
    }


    public CafStoreReference(String containerId, String assetId)
        throws DataStoreException
    {
        super(Arrays.asList(containerId, assetId));
        validate();
    }


    /**
     * @return the container ID from this CafStoreReference
     */
    public String getContainer()
    {
        return getIndex(0);
    }


    /**
     * @return the asset ID from this CafStoreReference
     */
    public String getAsset()
    {
        return getIndex(1);
    }


    /**
     * Check there are 2 components to this name and that they are both valid UUIDs.
     * @throws DataStoreException if this is not a valid CafStoreReference
     */
    private void validate()
        throws DataStoreException
    {
        if ( size() != 2 ) {
            throw new DataStoreException("Invalid reference, must consist of container id and asset id only");
        }

        for(String component:this){
            Matcher matcher = pattern.matcher(component);
            if(!matcher.find()){
                throw new DataStoreException("Invalid reference due to invalid characters");
            }
        }
    }
}
