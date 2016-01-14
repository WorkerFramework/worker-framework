package com.hpe.caf.worker.testing;

import java.util.Collection;
import java.util.Map;

/**
 * Created by mcmurrap on 14/01/2016.
 */
public class MetadataHelper {

    private MetadataHelper(){}

    public static String getMetadataValue(Collection<Map.Entry<String, String>> metadata, String key) {
        String value = "";
        if (metadata != null) {
            for (Map.Entry me : metadata) {
                if (key.equalsIgnoreCase(me.getKey().toString())) {
                    value = me.getValue().toString();
                    break;
                }
            }
        }

        return value;
    }

    public static void clearMetadataValue(Collection<Map.Entry<String, String>> metadata, String key) {
        if (metadata != null) {
            for (Map.Entry me : metadata) {
                if (key.equalsIgnoreCase(me.getKey().toString())) {
                    me.setValue("");
                    break;
                }
            }
        }
    }
}
