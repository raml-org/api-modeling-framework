package org.raml.amf.core.domain;

import api_modeling_framework.model.domain.ParsedAPIDocumentation;
import clojure.lang.IFn;
import org.raml.amf.core.exceptions.InvalidModelException;
import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by antoniogarrote on 04/05/2017.
 */


/**
 * Main EntryPoint of the description of HTTP RPC API
 */
public class APIDocumentation extends DomainModel {

    static {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN);
    }

    public APIDocumentation(ParsedAPIDocumentation rawModel) {
        super(rawModel);
    }

    /**
     * Build a new empty API Documentation for the provided URI
     * @param id
     */
    public APIDocumentation(String id) {
        super(Clojure.var(
                Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN,
                "api-modeling-framework.model.domain/map->ParsedAPIDocumentation"
                ).invoke(Clojure.map())
        );
        this.setId(id);
    }

    public String getTermsOfService() {
        return (String) this.wrapped().terms_of_service();
    }

    public void setTermsOfService(String termsOfService) {
        this.rawModel = Clojure.setKw(this.rawModel, "terms-of-service", termsOfService);
    }

    public String getBasePath() {
        Object res = this.wrapped().base_path();
        if (res != null)
            return (String) res;
        else
            return null;
    }

    public void setBasePath(String basePath) {
        this.rawModel = Clojure.setKw(this.rawModel, "base-path", basePath);
    }

    public String getHost() {
        Object res = this.wrapped().host();
        if (res != null)
            return (String) res;
        else
            return null;
    }

    public void setHost(String host) {
        this.rawModel = Clojure.setKw(this.rawModel, "host", host);
    }

    public String getVersion() {
        Object res = this.wrapped().version();
        if (res != null)
            return (String) res;
        else
            return null;
    }

    public void setVersion(String host) {
        this.rawModel = Clojure.setKw(this.rawModel, "version", host);
    }

    /**
     * URI Scheme for the paths in the API
     * @return
     */
    public String getScheme() {
        Object res = this.wrapped().scheme();
        if (res != null)
            return (String) res;
        else
            return null;
    }

    public void setScheme(String scheme) {
        this.rawModel = Clojure.setKw(this.rawModel, "scheme", scheme);
    }

    public List<String> getAccepts() {
        return (List<String>) this.wrapped().accepts();
    }

    public void setAccepts(List<String> accepts) {
        this.rawModel = Clojure.setKw(this.wrapped(), "accepts", Clojure.list(accepts));
    }

    public List<String> getContentTypes() {
        return (List<String>) this.wrapped().content_type();
    }

    public void setContentTypes(List<String> contentTypes) {
        this.rawModel = Clojure.setKw(this.wrapped(), "content-type", Clojure.list(contentTypes));
    }

    /**
     * List of HTTP headers in this unit
     * @return
     */
    public List<Header> getHeaders() {
        List headers =  (List) this.wrapped().headers();
        List<api_modeling_framework.model.domain.ParsedParameter> tmp = Clojure.toJavaList(headers);
        ArrayList<Header> eps = new ArrayList<>();
        for(api_modeling_framework.model.domain.ParsedParameter x : tmp) {
            Header parsed = new Header(x);
            eps.add(parsed);
        }

        return eps;
    }

    public void setHeaders(List<Header> headers) {
        ArrayList<Object> raws = new ArrayList<>();
        for(Header x : headers) {
            raws.add(x.clojureModel());
        }

        this.rawModel = Clojure.setKw(this.rawModel, "headers", Clojure.list(raws));
    }

    public List<Parameter> getParameters() {
        List parameters =  (List) this.wrapped().parameters();
        List<api_modeling_framework.model.domain.ParsedParameter> tmp = Clojure.toJavaList(parameters);
        ArrayList<Parameter> eps = new ArrayList<>();
        for(api_modeling_framework.model.domain.ParsedParameter x : tmp) {
            Parameter parsed = new Parameter(x);
            eps.add(parsed);
        }

        return eps;
    }

    public void setParameters(List<Parameter> parameters) {
        ArrayList<Object> raws = new ArrayList<>();
        for(Parameter x : parameters) {
            raws.add(x.clojureModel());
        }

        this.rawModel = Clojure.setKw(this.rawModel, "parameters", Clojure.list(raws));
    }

    /**
     * List of EndPoints declared in this API
     * @return
     * @throws InvalidModelException
     */
    public List<EndPoint> getEndpoints() throws InvalidModelException {
        List endpoints =  (List) this.wrapped().endpoints();
        List<api_modeling_framework.model.domain.ParsedEndPoint> tmp = Clojure.toJavaList(endpoints);
        ArrayList<EndPoint> eps = new ArrayList<>();
        for(api_modeling_framework.model.domain.ParsedEndPoint x : tmp) {
            EndPoint parsed = new EndPoint(x);
            eps.add(parsed);
        }

        return eps;
    }

    public void setEndPoints(List<EndPoint> operations) {
        ArrayList<Object> raws = new ArrayList<>();
        for(EndPoint x : operations) {
            raws.add(x.clojureModel());
        }

        this.rawModel = Clojure.setKw(this.rawModel, "endpoints", Clojure.list(raws));
    }

    protected ParsedAPIDocumentation wrapped() {
        return (ParsedAPIDocumentation) this.rawModel;
    }
}
