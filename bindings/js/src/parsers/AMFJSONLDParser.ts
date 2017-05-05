/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {BaseParser} from "./BaseParser";

const amf = require("api-modeling-framework");

/**
 * Wrapper class for the AMF OWL model parser, processes AMF model specification JSON-LD documents and generate the DocumentModel out of them
 */
export class AMFJSONLDParser extends BaseParser {
    protected parser(): any {
        return new amf.__GT_APIModelParser();
    }
}