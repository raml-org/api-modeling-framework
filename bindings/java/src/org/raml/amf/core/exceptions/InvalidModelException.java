package org.raml.amf.core.exceptions;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * Exception related to a native clojure model that is not matching the expected value
 */
public class InvalidModelException extends RuntimeException {

    public InvalidModelException(Exception ex) {
        super(ex);
    }
}
