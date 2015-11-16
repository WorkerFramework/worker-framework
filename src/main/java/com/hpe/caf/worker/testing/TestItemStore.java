package com.hpe.caf.worker.testing;


import java.util.HashMap;

/**
 * Created by ploch on 07/11/2015.
 */
public class TestItemStore {

    private final HashMap<String, TestItem> items = new HashMap<>();
    private final ExecutionContext context;

    public TestItemStore(ExecutionContext context) {
        this.context = context;
    }


    public TestItem findAndRemove(String id) {
        synchronized (items) {
            TestItem item = items.get(id);
            if (item != null) {
                items.remove(id,item );
                if (items.size() == 0) {
                    context.finishedSuccessfully();
                }
            }

            return item;
        }
    }

    public void store(String id, TestItem item) {
        synchronized (items) {
            items.put(id, item);
        }
    }
}
