package org.raml.amf.core.domain;

import api_modeling_framework.model.domain.ParsedEndPoint;
import api_modeling_framework.model.domain.ParsedOperation;
import org.raml.amf.core.exceptions.InvalidModelException;
import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * EndPoints contains information about a HTTP remote location where a number of API operations have been bound
 */
public class EndPoint extends DomainModel {

    static {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN);
    }

    public EndPoint(ParsedEndPoint rawModel) {
        super(rawModel);
    }

    /**
     * Builds a new EndPoint for the provided URI
     * @param id
     */
    public EndPoint(String id) {
        super(Clojure.var(
                Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN,
                "api-modeling-framework.model.domain/map->ParsedEndPoint"
                ).invoke(Clojure.map())
        );
        this.setId(id);
    }

    protected ParsedEndPoint wrapped() {
        return (ParsedEndPoint) rawModel;
    }

    /**
     * Path for the URL where the operations of the EndPoint are bound
     * @return
     */
    public String getPath() {
        return (String) this.wrapped().path();
    }

    public void setPath(String path) {
        this.rawModel = Clojure.setKw(this.wrapped(), "path", path);
    }

    /**
     * List of API Operations bound to this EndPoint
     * @return
     */
    public List<Operation> getSupportedOperations() {
        List operations =  (List) this.wrapped().supported_operations();
        List<api_modeling_framework.model.domain.ParsedOperation> tmp = Clojure.toJavaList(operations);
        ArrayList<Operation> eps = new ArrayList<>();
        for(api_modeling_framework.model.domain.ParsedOperation x : tmp) {
            Operation parsed = new Operation(x);
            eps.add(parsed);
        }

        return eps;
    }

    public void setSupportedOperations(List<Operation> operations) {
        ArrayList<Object> raws = new ArrayList<>();
        for(Operation x : operations) {
            raws.add(x.clojureModel());
        }

        this.rawModel = Clojure.setKw(this.rawModel, "supported-operations", Clojure.list(raws));
    }

}
