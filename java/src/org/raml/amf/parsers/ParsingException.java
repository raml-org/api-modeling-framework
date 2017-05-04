package org.raml.amf.parsers;

import java.io.IOException;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * Exception produced while parsing an input syntax for the AMF parser
 */
public class ParsingException extends IOException {

    public ParsingException(Exception ex) {
        super(ex);
    }

}
