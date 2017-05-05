/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {BaseGenerator} from "./BaseGenerator";
const amf = require("api-modeling-framework");

/**
 * Serialises the AMF model as an OpenAPI JSON document
 */
export class OpenAPIGenerator extends BaseGenerator {
    protected generator(): any {
        return new amf.__GT_OpenAPIGenerator();
    }
}