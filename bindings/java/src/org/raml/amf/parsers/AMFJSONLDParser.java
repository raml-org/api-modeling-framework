package org.raml.amf.parsers;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * Wrapper class for the AMF OWL model parser, processes AMF model specification JSON-LD documents and generate the DocumentModel out of them
 */
public class AMFJSONLDParser extends BaseParser {

    @Override
    protected String parserConstructor() {
        return "->APIModelParser";
    }
}
