package com.hpe.caf.worker.datastore.cs;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.Charset;
import java.util.List;

/**
 * The ReferenceComponents class provides access to the asset related reference components.
 */

public class ReferenceComponents {

    private final String containerId;
    private final String assetId;
    private final String queryString;

    public ReferenceComponents(String reference) {

        if (reference == null) {
            throw new IllegalArgumentException("Reference has not been supplied.");
        }

        //  Split reference to separate container/asset identifiers from named value part.
        String[] refComponents = reference.split("\\?");
        if (refComponents != null && refComponents.length > 0) {
            String[] containerAssetIds = refComponents[0].split("/");

            //  Identify container and asset ids if provided.
            if (containerAssetIds != null && containerAssetIds.length > 0) {
                containerId = containerAssetIds[0];
                if (containerAssetIds.length > 1) {
                    assetId = containerAssetIds[1];
                } else {
                    assetId = null;
                }
            } else {
                containerId = null;
                assetId = null;
            }

            //  Identify named value pairs if provided.
            if (refComponents.length > 1) {
                queryString = refComponents[1];
            } else {
                queryString = null;
            }
        } else {
            containerId = null;
            assetId = null;
            queryString = null;
        }
    }

    /**
     * @return the container ID from this Reference
     */
    public String getContainerId()
    {
        if (containerId != null && !containerId.isEmpty()) {
            return containerId;
        }
        return null;
    }

    /**
     * @return the asset ID from this Reference
     */
    public String getAssetId()
    {
        if (assetId != null && !assetId.isEmpty()) {
            return assetId;
        }
        return null;
    }

    /**
     * @return the name/value collection from the query string.
     */
    public List<NameValuePair> getNameValueCollection()
    {
        if (queryString != null && !queryString.isEmpty()) {
            return URLEncodedUtils.parse(queryString, Charset.forName("utf-8"));
        }
        return null;
    }
}
