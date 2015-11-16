package com.hpe.caf.worker.testing;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by ploch on 08/11/2015.
 */
public interface TestItemProvider {

    Collection<TestItem> getItems() throws IOException;
}
