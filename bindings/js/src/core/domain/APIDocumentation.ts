/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {DomainModel} from "./DomainModel";
import {Clojure} from "../../Clojure";
import {EndPoint} from "./EndPoint";
import {Parameter} from "./Parameter";
import {Header} from "./Header";

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

    public getVersion(): string | undefined {
        return Clojure.amf_domain.version(this.rawModel);
    }

    public setVersion(version: string | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("version"));
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

    public getParameters(): Parameter[] {
        const parametersCljs = Clojure.amf_domain.parameters(this.rawModel);
        return Clojure.cljsMap(parametersCljs, (e) => new Parameter(e));
    }

    public setParameters(parameters: Parameter[]) {
        const newParameters = Clojure.toCljsSeq(parameters.map(x => x.clojureModel()));
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("parameters"), newParameters);
    }

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