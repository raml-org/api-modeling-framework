/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {GenericOperationUnit} from "./GenericOperationUnit";
import {Clojure} from "../../Clojure";
import {DomainModel} from "./DomainModel";

/**
 * Information about the response returned by an operation, associated to a particular status
 */
export class Response extends GenericOperationUnit {

    /**
     * HTTP status code for the response
     * @return
     */
    public getStatusCode(): string | undefined {
        return Clojure.amf_domain.status_code(this.rawModel);
    }

    public setStatusCode(statusCode: string | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("status-code"), statusCode);
    }

    public static build(id: string): Response {
        return new Response(Clojure.amf.build(
            DomainModel.domain_builder,
            Clojure.amf_domain.map__GT_ParsedResponse,
            id));
    }

}