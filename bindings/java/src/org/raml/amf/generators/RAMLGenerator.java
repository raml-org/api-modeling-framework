package org.raml.amf.generators;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * Serialised the AMF model as a RAML YAML document
 */
public class RAMLGenerator extends BaseGenerator {

    @Override
    protected String generatorConstructor() {
        return "->RAMLGenerator";
    }
}
