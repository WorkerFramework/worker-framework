/*
 * Copyright 2022-2022 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.worker.testing;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@code TestItemStore} class responsible for storing and maintaining test cases - {@link TestItem} objects.
 * <p>
 * Test cases are provided by {@link TestItemProvider} and then stored in {@code TestItemStore}. After a task created using particular
 * test case {@code TestItem} is processed by a worker, {@code TestItem} is retrieved from the {@code TestItemStore} and used to validate
 * the worker result. After validation (processing) of test item, it is removed from {@code TestItemStore}.
 */
public class TestItemStore
{
    private final HashMap<String, TestItem> items = new HashMap<>();
    private final ExecutionContext context;

    /**
     * Instantiates a new Test item store.
     *
     * @param context the context
     */
    public TestItemStore(ExecutionContext context)
    {
        this.context = context;
    }

    /**
     * Size int.
     *
     * @return the int
     */
    public int size()
    {
        synchronized (items) {
            return items.size();
        }
    }

    /**
     * Find and remove test item.
     *
     * @param id the id
     * @return the test item
     */
    public TestItem findAndRemove(String id)
    {
        synchronized (items) {
            TestItem item = items.get(id);
            if (item != null) {
                items.remove(id, item);
            }

            return item;
        }
    }

    /**
     * Store.
     *
     * @param id the id
     * @param item the item
     */
    public void store(String id, TestItem item)
    {
        synchronized (items) {
            items.put(id, item);
        }
    }

    /**
     * Find test item.
     *
     * @param id the id
     * @return the test item
     */
    public TestItem find(String id)
    {
        synchronized (items) {
            TestItem item = items.get(id);
            if (item == null) {
                Optional<String> firstKey = items.keySet().stream().filter(id::startsWith).findFirst();
                boolean firstKeyPresent = firstKey.isPresent();
                String actualId = "";
                // If the id exists in the set of keys assign it to the item to be returned
                if (firstKeyPresent) {
                    actualId = firstKey.get();
                    item = items.get(actualId);
                    return item;
                }
                // If the id is not contained within the key then look for the ID in the values
                Optional<TestItem> firstValue = items.values().stream().filter(Objects::nonNull).filter(testItem -> testItem.getInputIdentifier().equals(id)).findFirst();
                boolean firstValuePresent = firstValue.isPresent();
                if (firstValuePresent) {
                    actualId = firstValue.get().getTag();
                    item = items.get(actualId);
                    return item;
                }
                return null;
            }
            return item;
        }
    }

    /**
     * Getter for property 'items'.
     *
     * @return Value for property 'items'.
     */
    public Map<String, TestItem> getItems()
    {
        return items;
    }

    /**
     * Remove.
     *
     * @param id the id
     */
    public void remove(String id)
    {
        synchronized (items) {
            //        if (item != null) {

            TestItem remove = items.remove(id);
        }
    }
}
