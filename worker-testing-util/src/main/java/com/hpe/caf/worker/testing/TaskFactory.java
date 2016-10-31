package com.hpe.caf.worker.testing;

import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.DataStoreException;

import java.io.FileNotFoundException;

/**
 * Created by ploch on 04/11/2015.
 */
public interface TaskFactory<TInput> {

    byte[] createProduct(String taskId, TInput input) throws FileNotFoundException, DataStoreException, CodecException;

}
