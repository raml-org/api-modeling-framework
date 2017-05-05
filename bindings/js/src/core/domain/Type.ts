/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {DomainModel} from "./DomainModel";
import {Clojure} from "../../Clojure";

export type JSONLD = Object

/**
 * Data Shape that describing a set of constraints over an operation unit payload
 */
export class Type extends DomainModel {

    /**
     * JSON-LD data structure containing a SHACL shape that can be used to validate payloads for this operation unit
     * @return
     */
    public getShape(): JSONLD | undefined {
        return Clojure.cljsToJs(Clojure.amf_domain.shape(this.rawModel));
    }

    /**
     * Sets the SHACL shape for the payloads of this operation unit
     * @param shaclShape valid SHACL shape encoded as JSON-LD string
     */
    public setShape(shape: JSONLD | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("shape"), Clojure.jsToCljs(shape));
    }


    public static build(id: string): Type {
        return new Type(Clojure.amf.build(
            DomainModel.domain_builder,
            Clojure.amf_domain.map__GT_ParsedType,
            id));
    }
}