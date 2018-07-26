/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {BaseGenerator} from "./BaseGenerator";
import {Clojure} from "../Clojure";

/**
 * Serialises the AMF model as an OpenAPI JSON document
 */
export class OpenAPIGenerator extends BaseGenerator {
    protected generator(): any {
        return new Clojure.amf.__GT_OpenAPIGenerator();
    }
}