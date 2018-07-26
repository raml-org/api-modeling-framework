/**
 * Created by antoniogarrote on 05/05/2017.
 */
import {DomainModel} from "./DomainModel";
import {Request} from "./Request";
import {Response} from "./Response";
import {Clojure} from "../../Clojure";

/**
 * A unit of business logic exposed by the API. Operations can be invoked using the associated HTTP method.
 */
export class Operation extends DomainModel {

    /**
     * HTTP method that must be used to invoke the Operation
     * @return
     */
    public getMethod(): string | undefined {
        return Clojure.amf_domain.method(this.rawModel);
    }

    public setMethod(method: string | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("method"), method);
    }

    /**
     * HTTP scheme that must be used to invoke the operation, overrides the default in APIDocumentation
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

    /**
     * HTTP media-types accepted by the operation, overrides the default in APIDocumentation
     */
    public setAccepts(mediaTypes: string[]) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("accepts"), Clojure.jsToCljs(mediaTypes));
    }

    /**
     * HTTP media-types returned by the operation, overrides the default in APIDocumentation
     */
    public getContentTypes(): string[] {
        return Clojure.cljsToJs(Clojure.amf_domain.content_type(this.rawModel));
    }

    public setContentTypes(mediaTypes: string[]) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("content-type"), Clojure.jsToCljs(mediaTypes));
    }

    /**
     * Request information for the operation
     * @return
     */
    public getRequest(): Request | undefined {
        let request = Clojure.amf_domain.request(this.rawModel);
        if (request != null) {
            return new Request(request);
        }
    }

    public setRequest(request: Request | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("request"), request != null ? request.clojureModel() : null);
    }

    /**
     * List of responses for different HTTP status codes supported by this operation
     * @return
     */
    public getResponses(): Response[] {
        let responses = Clojure.amf_domain.responses(this.rawModel);
        return Clojure.cljsMap(responses, (e) => new Response(e));
    }

    public setResponses(responses: Response[]) {
        const newResponses = responses.map(e => e.clojureModel());
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("request"), Clojure.toCljsSeq(newResponses));
    }

    /**
     * Builds a new empty Operation with the provide URI
     * @param id
     */
    public static build(id: string): Operation {
        return new Operation(Clojure.amf.build(
            DomainModel.domain_builder,
            Clojure.amf_domain.map__GT_ParsedOperation,
            id));
    }
}