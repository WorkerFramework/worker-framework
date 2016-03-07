package com.hpe.caf.worker.messagebuilder;

import com.hpe.caf.messagebuilder.Document;

/**
 * POJO implementation of Document interface for use with testing ExampleWorkerMessageBuilder
 */
public class TestDocumentImpl implements Document {
    private String storageReference;

    public void setStorageReference(String storageReference){
        this.storageReference = storageReference;
    }
    public String getStorageReference(){
        return this.storageReference;
    }
}
