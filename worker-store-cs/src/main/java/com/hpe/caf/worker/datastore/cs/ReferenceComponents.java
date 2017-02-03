/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.worker.datastore.cs;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The ReferenceComponents class provides access to the asset related reference components.
 */

public class ReferenceComponents {

    private final String reference;
    private final String queryString;

    public ReferenceComponents(String ref) {

        if (ref == null) {
            throw new IllegalArgumentException("Reference has not been supplied.");
        }

        //  The supplied 'ref' value could comprise a containerId, assetId and a name value collection.
        String[] refComponents = ref.split("\\?");
        if (refComponents.length > 0) {

            //  Identify containerId or containerId/assetId.
            reference = refComponents[0];

            //  Identify named value pairs if provided.
            if (refComponents.length > 1) {
                queryString = refComponents[1];
            } else {
                queryString = null;
            }
        } else {
            reference = null;
            queryString = null;
        }
    }

    /**
     * @return the reference only (i.e. containerId or containerId/assetId)
     */
    public String getReference()
    {
        return reference;
    }

    /**
     * @return the named value from the the name value collection
     */
    public String getNamedValue(String name)
    {
        String returnValue = null;

        if (queryString != null) {
            List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8);
            for (NameValuePair nvp : nameValuePairs) {
                if (nvp.getName().toLowerCase().equals(name.toLowerCase())) {
                    returnValue = nvp.getValue();
                    break;
                }
            }
        }

        return returnValue;
    }

    public static ReferenceComponents parseReference(String ref) {
        return new ReferenceComponents(ref);
    }
}
