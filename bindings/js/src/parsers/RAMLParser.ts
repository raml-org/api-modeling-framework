/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {BaseParser} from "./BaseParser";

const amf = require("api-modeling-framework");

/**
 * Wrapper class for the AMF RAML parser, processes RAML specification documents and generate the DocumentModel out of them
 */
export class RAMLParser extends BaseParser {
    protected parser(): any {
        return new amf.__GT_RAMLParser();
    }
}