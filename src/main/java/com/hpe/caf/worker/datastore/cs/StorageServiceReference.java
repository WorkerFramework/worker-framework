package com.hpe.caf.worker.datastore.cs;

/**
 * A typed representation of the string reference format used by the StorageService Datastore
 */
public class StorageServiceReference {
    private String assetId;
    private String containerId;
    private Long revId;

    public StorageServiceReference() {
    }

    public String getAssetId() {
        return this.assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getContainerId() {
        return this.containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public Long getRevId() {
        return this.revId;
    }

    public void setRevId(Long revId) {
        this.revId = revId;
    }
}
