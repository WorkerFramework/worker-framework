package com.hpe.caf.worker.datastore.cs;

/**
 * The Reference class is responsible for parsing asset related references.
 */
public class Reference {

    private final String reference;

    public Reference(String reference) {

        if (reference == null) {
            throw new IllegalArgumentException("Reference has not been supplied.");
        }

        this.reference = reference;
    }

    public ReferenceComponents parse() {
        return new ReferenceComponents(reference);
    }
}
