package com.hpe.caf.worker.testing;


import java.util.HashMap;
import java.util.Optional;

/**
 * Created by ploch on 07/11/2015.
 */
public class TestItemStore {

    private final HashMap<String, TestItem> items = new HashMap<>();
    private final ExecutionContext context;

    public TestItemStore(ExecutionContext context) {
        this.context = context;
    }

    public int size() {
        synchronized (items) {
            return items.size();
        }
    }

    public TestItem findAndRemove(String id) {
        synchronized (items) {
            TestItem item = items.get(id);
            if (item != null) {
                items.remove(id,item );
            }

            return item;
        }
    }

    public void store(String id, TestItem item) {
        synchronized (items) {
            items.put(id, item);
        }
    }

    public TestItem find(String id) throws Exception {
        synchronized (items) {
            TestItem item = items.get(id);
            if (item == null) {
                Optional<String> first = items.keySet().stream().filter(key -> id.startsWith(key)).findFirst();
                boolean present = first.isPresent();
                if (!present) return null;
                /*if (search.count() > 1) {
                    throw new Exception("Multiple keys matching the same task id");
                }*/
                String actualId = first.get();

                item = items.get(actualId);
            }
            return item;
        }
    }

    public void remove(String id) {
        synchronized (items) {
            //        if (item != null) {

            TestItem remove = items.remove(id);
        }
    }

}
