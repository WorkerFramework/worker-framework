package com.hpe.caf.worker.testing.data;


import com.hpe.caf.worker.testing.TestItem;

/**
 * The Serializer interface responsible for serializing and deserializing {@link TestItem} (test cases).
 * It also supplies an extension for test case file.
 */
public interface Serializer {

    /**
     * @return
     */
    TestItem deserialize();

    /**
     * Serializes test case descriptor.
     * @param testItem Test case descriptor to serialize
     * @return Test case descriptor serialized to byte array
     */
    byte[] serialize(TestItem testItem);
}
