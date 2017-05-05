/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {DomainModel} from "./DomainModel";
import {Clojure} from "../../Clojure";

export type ParameterKind = "header" | "domain" | "path" | "query";

/**
 * Parameters include all kind of input or output information that is requested or returned by an API Operation that
 * are not encoded in the Request or Response payloads.
 * Parameters can be located in HTTP headers or in the domain, path or arguments of the request URL.
 */
export abstract class GenericParameter extends DomainModel {

    public getRequired(): boolean {
        return Clojure.amf_domain.host(this.rawModel) || false;
    }

    public setRequired(required: boolean) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("required"), required);
    }

    public setParameterKindInternal(parameterKind: ParameterKind) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("parameter-kind"), parameterKind);
    }

}