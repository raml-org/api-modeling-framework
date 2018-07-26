/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {BaseGenerator} from "./BaseGenerator";
import {Clojure} from "../Clojure";

/**
 * Generator that exports the AMF model as a JSON-LD document
 */
export class AMFJSONDGenerator extends BaseGenerator {
    protected generator(): any {
        return new Clojure.amf.__GT_APIModelGenerator();
    }
}