package org.raml.amf.generators;

/**
 * Created by antoniogarrote on 04/05/2017.
 */


/**
 * Serialises the AMF model as an OpenAPI JSON document
 */
public class OpenAPIGenerator extends BaseGenerator {

    @Override
    protected String generatorConstructor() {
        return "->OpenAPIGenerator";
    }
}
