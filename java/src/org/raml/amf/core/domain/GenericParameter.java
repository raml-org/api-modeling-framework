package org.raml.amf.core.domain;

import api_modeling_framework.model.domain.ParsedParameter;
import api_modeling_framework.model.domain.ParsedType;
import org.raml.amf.core.exceptions.InvalidModelException;
import org.raml.amf.utils.Clojure;

/**
 * Created by antoniogarrote on 04/05/2017.
 */


/**
 * Parameters include all kind of input or output information that is requested or returned by an API Operation that
 * are not encoded in the Request or Response payloads.
 * Parameters can be located in HTTP headers or in the domain, path or arguments of the request URL.
 */
public abstract class GenericParameter extends DomainModel {

    static {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN);
    }

    public GenericParameter(ParsedParameter rawModel) {
        super(rawModel);
    }

    public Boolean getRequired() {
        return (Boolean) this.wrapped().required();
    }

    public void setRequired(Boolean required) {
        this.rawModel = Clojure.setKw(this.rawModel, "required", required);
    }

    public Type getSchema() {
        ParsedType type =  (ParsedType) this.wrapped().shape();
        return new Type(type);
    }

    protected void setParameterKindInternal(String parameterKind) {
        this.rawModel = Clojure.setKw(this.rawModel, "parameter-kind", parameterKind);
    }

    public void setSchema(Type type) {
        this.rawModel = Clojure.setKw(this.wrapped(), "shape", type.clojureModel());
    }

    protected ParsedParameter wrapped() {
        return (ParsedParameter) this.rawModel;
    }
}
