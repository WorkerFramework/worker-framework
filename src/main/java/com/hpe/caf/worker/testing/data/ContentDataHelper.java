package com.hpe.caf.worker.testing.data;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreSource;
import com.hpe.caf.util.ref.DataSourceException;
import com.hpe.caf.util.ref.ReferencedData;

import java.io.InputStream;

/**
 * Created by ploch on 08/12/2015.
 */
public class ContentDataHelper {

    public static InputStream retrieveReferencedData(DataStore dataStore, Codec codec, ReferencedData referencedData) throws DataSourceException {
        DataStoreSource source = new DataStoreSource(dataStore, codec);
        
        return referencedData.acquire(source);
    }

}
