/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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

import java.util.Collection;
import java.util.Map;

/**
 * Created by mcmurrap on 14/01/2016.
 */
public class MetadataHelper
{
    private MetadataHelper()
    {
    }

    public static String getMetadataValue(Collection<Map.Entry<String, String>> metadata, String key)
    {
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

    public static void clearMetadataValue(Collection<Map.Entry<String, String>> metadata, String key)
    {
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
