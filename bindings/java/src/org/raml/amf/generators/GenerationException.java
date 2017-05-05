package org.raml.amf.generators;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

public class GenerationException extends Throwable {
    public GenerationException(Exception rawModel) {
        super(rawModel);
    }
}
