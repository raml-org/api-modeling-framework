/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {GenericOperationUnit} from "./GenericOperationUnit";
import {Clojure} from "../../Clojure";
import {DomainModel} from "./DomainModel";
import {Parameter} from "./Parameter";

/**
 * HTTP request information reuqired by an API Operation
 */
export class Request extends GenericOperationUnit {

    public getParameters(): Parameter[] {
        const parametersCljs = Clojure.amf_domain.parameters(this.rawModel);
        return Clojure.cljsMap(parametersCljs, (e) => new Parameter(e));
    }

    public setParameters(parameters: Parameter[]) {
        const newParameters = Clojure.toCljsSeq(parameters.map(x => x.clojureModel()));
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("parameters"), newParameters);
    }

    public static build(id: string): Request {
        return new Request(Clojure.amf.build(
            DomainModel.domain_builder,
            Clojure.amf_domain.map__GT_ParsedRequest,
            id));
    }

}