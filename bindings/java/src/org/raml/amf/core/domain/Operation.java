package org.raml.amf.core.domain;

import api_modeling_framework.model.domain.ParsedOperation;
import api_modeling_framework.model.domain.ParsedRequest;
import org.raml.amf.core.exceptions.InvalidModelException;
import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * A unit of business logic exposed by the API. Operations can be invoked using the associated HTTP method.
 */
public class Operation extends DomainModel {

    static {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN);
    }

    public Operation(api_modeling_framework.model.domain.ParsedOperation rawModel) throws InvalidModelException {
        super(rawModel);
    }

    /**
     * Builds a new empty Operation with the provide URI
     * @param id
     */
    public Operation(String id) {
        super(Clojure.var(
                Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN,
                "api-modeling-framework.model.domain/map->ParsedOperation"
                ).invoke(Clojure.map())
        );
        this.setId(id);
    }

    /**
     * HTTP method that must be used to invoke the Operation
     * @return
     */
    public String getMethod() {
        Object res = this.wrapped().method();
        if (res != null) {
            return (String) res;
        } else {
            return null;
        }
    }

    public void setMethod(String method) {
        this.rawModel = Clojure.setKw(this.wrapped(), "method", method);
    }

    /**
     * HTTP scheme that must be used to invoke the operation, overrides the default in APIDocumentation
     * @return
     */
    public String getScheme() {
        Object res = this.wrapped().scheme();
        if (res != null) {
            return (String) res;
        } else {
            return null;
        }
    }

    public void setScheme(String scheme) {
        this.rawModel = Clojure.setKw(this.rawModel, "scheme", scheme);
    }

    /**
     * HTTP media-types accepted by the operation, overrides the default in APIDocumentation
     */
    public List<String> getAccepts() {
        return (List<String>) this.wrapped().accepts();
    }

    public void setAccepts(List<String> accepts) {
        this.rawModel = Clojure.setKw(this.wrapped(), "accepts", Clojure.list(accepts));
    }

    /**
     * HTTP media-types returned by the operation, overrides the default in APIDocumentation
     */
    public List<String> getContentTypes() {
        return (List<String>) this.wrapped().content_type();
    }

    public void setContentTypes(List<String> contentTypes) {
        this.rawModel = Clojure.setKw(this.wrapped(), "content-type", Clojure.list(contentTypes));
    }

    /**
     * List of responses for different HTTP status codes supported by this operation
     * @return
     */
    public List<Response> getResponses() {
        List responses =  (List) this.wrapped().responses();
        List<api_modeling_framework.model.domain.ParsedResponse> tmp = Clojure.toJavaList(responses);
        ArrayList<Response> eps = new ArrayList<>();
        for(api_modeling_framework.model.domain.ParsedResponse x : tmp) {
            Response parsed = new Response(x);
            eps.add(parsed);
        }

        return eps;
    }

    public void setResponses(List<Operation> responses) {
        ArrayList<Object> raws = new ArrayList<>();
        for(Operation x : responses) {
            raws.add(x.clojureModel());
        }

        this.rawModel = Clojure.setKw(this.wrapped(), "responses", Clojure.list(raws));
    }

    /**
     * Request information for the operation
     * @return
     */
    public Request getRequest() {
        Object res = this.wrapped().request();
        if (res != null) {
            ParsedRequest request = (ParsedRequest) res;
            return new Request(request);
        } else {
            return null;
        }
    }

    public void setRequest(Request request) {
        this.rawModel = Clojure.setKw(this.wrapped(), "request", request.clojureModel());
    }

    private ParsedOperation wrapped() {
        return (ParsedOperation) this.rawModel;
    }

}
