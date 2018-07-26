/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {Clojure} from "../../Clojure";
import {DomainModel} from "./DomainModel";
import {GenericParameter, ParameterKind} from "./GenericParameter";

/**
 * HTTP parameter other than a header. It can be located in the domain, path or query
 */
export class Parameter extends GenericParameter {

    public static build(id: string, parameterKind: ParameterKind): Parameter {
        return new Parameter(Clojure.amf.build(
            DomainModel.domain_builder,
            Clojure.amf_domain.map__GT_ParsedParameter,
            id));
    }
}