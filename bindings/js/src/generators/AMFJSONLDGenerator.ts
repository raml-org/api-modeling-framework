/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {BaseGenerator} from "./BaseGenerator";
const amf = require("api-modeling-framework");

/**
 * Generator that exports the AMF model as a JSON-LD document
 */
export class AMFJSONDGenerator extends BaseGenerator {
    protected generator(): any {
        return new amf.__GT_APIModelGenerator();
    }
}