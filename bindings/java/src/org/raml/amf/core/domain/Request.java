package org.raml.amf.core.domain;

import api_modeling_framework.model.domain.ParsedRequest;
import org.raml.amf.core.exceptions.InvalidModelException;
import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * HTTP request information reuqired by an API Operation
 */
public class Request extends GenericOperationUnit {

    static {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN);
    }

    public Request(ParsedRequest request) throws InvalidModelException {
        super(request);
    }

    public Request(String id) {
        super(Clojure.var(
                Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN,
                "api-modeling-framework.model.domain/map->ParsedRequest"
                ).invoke(Clojure.map())
        );
        this.setId(id);
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

    private ParsedRequest wrapped() {
        return (ParsedRequest) this.rawModel;
    }

}
