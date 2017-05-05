package org.raml.amf.core.exceptions;

import java.net.URL;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * Exception due to a reference to an unknown model
 */
public class UnknownModelReferenceException extends Exception {
    private URL uknownReference;

    public UnknownModelReferenceException(URL reference) {
        super("Cannot find model with reference " + reference.toString());
        this.uknownReference = reference;
    }

    public URL getUknownReference() {
        return uknownReference;
    }
}
