package com.hpe.caf.worker.datastore.cs;


import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.util.IOUtils;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.worker.datastore.s3.S3DataStore;
import com.hpe.caf.worker.datastore.s3.S3DataStoreConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


public class S3DataStoreTestIT
{
    S3DataStoreConfiguration s3DataStoreConfiguration;


    public S3DataStoreTestIT(){
        s3DataStoreConfiguration = new S3DataStoreConfiguration();

        s3DataStoreConfiguration.setProxyProtocol("HTTP");
        s3DataStoreConfiguration.setProxyHost("proxy.sdc.hp.com");
        s3DataStoreConfiguration.setProxyPort(8080);

        s3DataStoreConfiguration.setBucketName("andyreid-bucket");
        s3DataStoreConfiguration.setAccessKey("AKIAILPGEXAERZOJHC6Q");
        s3DataStoreConfiguration.setSecretKey("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");
    }


    /** Test that a complete reference string can be processed **/
    @Test
    public void testEverything()
        throws DataStoreException, IOException
    {
        S3DataStore s3DataStore = new S3DataStore(s3DataStoreConfiguration);
        String testString = "simple test";

        String reference = s3DataStore.store(testString.getBytes(), "/testing");

        try(InputStream inputStream = s3DataStore.retrieve(reference)){
            String result = IOUtils.toString(inputStream);
            Assert.assertEquals(result, testString);
        }

        s3DataStore.delete(reference);

        try(InputStream inputStream = s3DataStore.retrieve(reference)){
            String result = IOUtils.toString(inputStream);
            Assert.assertEquals(result, testString);
        }
        catch (DataStoreException ex){
        }
    }
}
