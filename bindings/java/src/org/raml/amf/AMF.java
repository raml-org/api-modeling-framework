package org.raml.amf;

import org.raml.amf.generators.AMFJSONLDGenerator;
import org.raml.amf.generators.OpenAPIGenerator;
import org.raml.amf.generators.RAMLGenerator;
import org.raml.amf.parsers.AMFJSONLDParser;
import org.raml.amf.parsers.OpenAPIParser;
import org.raml.amf.parsers.RAMLParser;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * Facade class providing access to the main IO facilities in the library
 */
public class AMF {

    /**
     * Builds a RAML to AMF parser
     * @return
     */
    public static RAMLParser RAMLParser() {
        return new RAMLParser();
    }

    /**
     * Builds an OpenAPI to AMF parser
     * @return
     */
    public static OpenAPIParser OpenAPIParser() {
        return new OpenAPIParser();
    }

    /**
     * Builds a AMF encoded JSON-LD to AMF parser
     * @return
     */
    public static AMFJSONLDParser JSONLDParser() {
        return new AMFJSONLDParser();
    }

    /**
     * Builds a AMF to RAML generator
     * @return
     */
    public static RAMLGenerator RAMLGenerator() {
        return new RAMLGenerator();
    }

    /**
     * Builds a AMF to OpenAPI generator
     * @return
     */
    public static OpenAPIGenerator OpenAPIGenerator() {
        return new OpenAPIGenerator();
    }

    /**
     * Builds a AMF to JSON-LD generator
     * @return
     */
    public static AMFJSONLDGenerator JSONLDGenerator() {
        return new AMFJSONLDGenerator();
    }
}
