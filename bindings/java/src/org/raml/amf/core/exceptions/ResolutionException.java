package org.raml.amf.core.exceptions;

import org.raml.amf.core.document.DocumentModel;

/**
 * Created by antoniogarrote on 04/05/2017.
 */
public class ResolutionException extends Exception {
    private final DocumentModel model;

    public ResolutionException(DocumentModel model, Exception ex) {
        super("Error resolving model", ex);
        this.model = model;
    }

    public DocumentModel getModel() {
        return model;
    }
}
