package org.raml.amf.parsers;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * Wrapper class for the AMF OpenAPI parser, processes OpenAPI specification documents and generate the DocumentModel out of them
 */
public class OpenAPIParser extends BaseParser {
    @Override
    protected String parserConstructor() {
        return "->OpenAPIParser";
    }
}
