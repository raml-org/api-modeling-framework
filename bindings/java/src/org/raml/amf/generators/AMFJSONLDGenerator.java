package org.raml.amf.generators;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * Generator that exports the AMF model as a JSON-LD document
 */
public class AMFJSONLDGenerator extends BaseGenerator {

    @Override
    protected String generatorConstructor() {
        return "->APIModelGenerator";
    }
}
