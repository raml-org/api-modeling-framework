/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {DomainModel} from "./DomainModel";
import {Header} from "./Header";
import {Clojure} from "../../Clojure";
import {Payload} from "./Payload";

/**
 * Base class for the Request and Response of an API
 */
export abstract class GenericOperationUnit extends DomainModel {

    /**
     * List of HTTP headers in this unit
     * @return
     */
    public getHeaders(): Header[] {
        const headersCljs = Clojure.amf_domain.headers(this.rawModel);
        return Clojure.cljsMap(headersCljs, (e) => new Header(e));
    }

    public setHeaders(headers: Header[]) {
        const newHeaders = Clojure.toCljsSeq(headers.map(x => x.clojureModel()));
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("headers"), newHeaders);
    }

    /**
     * List of Payloads in the unit
     * @return
     */
    public getPayloads(): Payload[] {
        const payloadsCljs = Clojure.amf_domain.payloads(this.rawModel);
        return Clojure.cljsMap(payloadsCljs, (e) => new Payload(e));
    }

    public setPayloads(payloads: Payload[]) {
        const newPayloads = Clojure.toCljsSeq(payloads.map(x => x.clojureModel()));
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("payloads"), newPayloads);
    }

}