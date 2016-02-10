package com.hpe.caf.worker.testing;


import java.util.HashMap;
import java.util.Optional;

/**
 * The {@code TestItemStore} class responsible for storing
 * and maintaining test cases - {@link TestItem} objects.
 * <p>Test cases are provided by {@link TestItemProvider} and then
 * stored in {@code TestItemStore}. After a task created using
 * particular test case {@code TestItem} is processed by a worker,
 * {@code TestItem} is retrieved from the {@code TestItemStore} and
 * used to validate the worker result.
 * After validation (processing) of test item, it is removed from
 * {@code TestItemStore}.
 */
public class TestItemStore {

    private final HashMap<String, TestItem> items = new HashMap<>();
    private final ExecutionContext context;

    /**
     * Instantiates a new Test item store.
     *
     * @param context the context
     */
    public TestItemStore(ExecutionContext context) {
        this.context = context;
    }

    /**
     * Size int.
     *
     * @return the int
     */
    public int size() {
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
    public TestItem findAndRemove(String id) {
        synchronized (items) {
            TestItem item = items.get(id);
            if (item != null) {
                items.remove(id,item );
            }

            return item;
        }
    }

    /**
     * Store.
     *
     * @param id   the id
     * @param item the item
     */
    public void store(String id, TestItem item) {
        synchronized (items) {
            items.put(id, item);
        }
    }

    /**
     * Find test item.
     *
     * @param id the id
     * @return the test item
     * @throws Exception the exception
     */
    public TestItem find(String id) {
        synchronized (items) {
            TestItem item = items.get(id);
            if (item == null) {
                Optional<String> first = items.keySet().stream().filter(id::startsWith).findFirst();
                boolean present = first.isPresent();
                if (!present) return null;

                String actualId = first.get();

                item = items.get(actualId);
            }
            return item;
        }
    }

    /**
     * Remove.
     *
     * @param id the id
     */
    public void remove(String id) {
        synchronized (items) {
            //        if (item != null) {

            TestItem remove = items.remove(id);
        }
    }

}
