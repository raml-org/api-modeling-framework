/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {BaseParser} from "./BaseParser";
import {Clojure} from "../Clojure";

/**
 * Wrapper class for the AMF OpenAPI parser, processes OpenAPI specification documents and generate the DocumentModel out of them
 */
export class OpenAPIParser extends BaseParser {
    protected parser(): any {
        return new Clojure.amf.__GT_OpenAPIParser();
    }
}