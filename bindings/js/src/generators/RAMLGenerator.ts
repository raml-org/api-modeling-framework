/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {BaseGenerator} from "./BaseGenerator";
import {Clojure} from "../Clojure";

/**
 * Serialised the AMF model as a RAML YAML document
 */
export class RAMLGenerator extends BaseGenerator {
    protected generator(): any {
        return new Clojure.amf.__GT_RAMLGenerator();
    }
}