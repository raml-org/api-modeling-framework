package org.raml.amf.parsers;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * Wrapper class for the AMF RAML parser, processes RAML specification documents and generate the DocumentModel out of them
 */
public class RAMLParser extends BaseParser {

    @Override
    protected String parserConstructor() {
        return "->RAMLParser";
    }
}
