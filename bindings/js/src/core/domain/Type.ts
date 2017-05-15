/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {DomainModel} from "./DomainModel";
import {Clojure} from "../../Clojure";
import {Shape} from "./shapes/Shape";

export type JSONLD = Object

/**
 * Type information including a shape describing a set of constraints over an operation unit payload
 */
export class Type extends DomainModel {

    /**
     * JSON-LD data structure containing a SHACL shape that can be used to validate payloads for this operation unit
     * @return
     */
    public getJSONLDShape(): JSONLD | undefined {
        return Clojure.cljsToJs(Clojure.amf_domain.shape(this.rawModel));
    }

    /**
     * Sets the SHACL shape for the payloads of this operation unit
     * @param shaclShape valid SHACL shape encoded as JSON-LD string
     */
    public setJSONLDShape(shape: JSONLD | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("shape"), Clojure.jsToCljs(shape));
    }


    /**
     * Shape object for this type
     * @returns {Shape}
     */
    public getShape(): Shape | undefined {
        let jsonld = this.getJSONLDShape();
        if (jsonld != null) {
            return Shape.fromRawModel(jsonld);
        }
    }

    public setShape(shape: Shape | undefined) {
        this.setJSONLDShape(shape.clojureModel());
    }

    public static build(id: string): Type {
        return new Type(Clojure.amf.build(
            DomainModel.domain_builder,
            Clojure.amf_domain.map__GT_ParsedType,
            id));
    }
}