/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {BaseGenerator} from "./BaseGenerator";
const amf = require("api-modeling-framework");

/**
 * Serialised the AMF model as a RAML YAML document
 */
export class RAMLGenerator extends BaseGenerator {
    protected generator(): any {
        return new amf.__GT_RAMLGenerator();
    }
}