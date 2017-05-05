/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {DomainModel} from "./DomainModel";
import {Clojure} from "../../Clojure";
import {EndPoint} from "./EndPoint";

/**
 * Main EntryPoint of the description of HTTP RPC API
 */
export class APIDocumentation extends DomainModel {

    public getTermsOfService(): string | undefined {
        return Clojure.amf_domain.terms_of_service(this.rawModel);
    }

    public setTermsOfService(termsOfService: string | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("terms-of-service"), termsOfService);
    }

    public getBasePath(): string | undefined {
        return Clojure.amf_domain.base_path(this.rawModel);
    }

    public setBasePath(basePath: string | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("base-path"), basePath);
    }

    public getHost(): string | undefined {
        return Clojure.amf_domain.host(this.rawModel);
    }

    public setHost(host: string | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("host"), host);
    }

    /**
     * URI Scheme for the paths in the API
     * @return
     */
    public getScheme(): string | undefined {
        return Clojure.amf_domain.scheme(this.rawModel);
    }

    public setScheme(scheme: string | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("scheme"), scheme);
    }

    public getAccepts(): string[] {
        return Clojure.cljsToJs(Clojure.amf_domain.accepts(this.rawModel));
    }

    public setAccepts(mediaTypes: string[]) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("accepts"), Clojure.jsToCljs(mediaTypes));
    }

    public getContentTypes(): string[] {
        return Clojure.cljsToJs(Clojure.amf_domain.content_type(this.rawModel));
    }

    public setContentTypes(mediaTypes: string[]) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("content-type"), Clojure.jsToCljs(mediaTypes));
    }

    /**
     * List of EndPoints declared in this API
     * @return
     * @throws InvalidModelException
     */
    public getEndPoints(): EndPoint[] {
        const endpointsCljs = Clojure.amf_domain.endpoints(this.rawModel);
        return Clojure.cljsMap(endpointsCljs, (e) => new EndPoint(e));
    }

    public setEndPoints(endpoints: EndPoint[]) {
        const newEndpoints = Clojure.toCljsSeq(endpoints.map(x => x.clojureModel()));
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("endpoints"), newEndpoints);
    }

    /**
     * Build a new empty API Documentation for the provided URI
     * @param id
     */
    public static build(id: string): APIDocumentation {
        return new APIDocumentation(Clojure.amf.build(
            DomainModel.domain_builder,
            Clojure.amf_domain.map__GT_ParsedAPIDocumentation,
        id));
    }
}