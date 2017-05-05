package org.raml.amf.core.domain;

import api_modeling_framework.model.domain.ParsedParameter;
import org.raml.amf.core.exceptions.InvalidModelException;
import org.raml.amf.utils.Clojure;

/**
 * Created by antoniogarrote on 04/05/2017.
 */
public class Parameter extends GenericParameter {

    static {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN);
    }

    public Parameter(ParsedParameter rawModel) {
        super(rawModel);
    }

    public Parameter(String id, String parameterKind) {
        super((ParsedParameter) Clojure.var(
                Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN,
                "api-modeling-framework.model.domain/map->ParsedParameter"
                ).invoke(Clojure.map())
        );
        this.setId(id);
        this.setParameterKindInternal(parameterKind);
    }

    protected void setParameterKind(String parameterKind) {
        setParameterKindInternal(parameterKind);
    }

    public String getParameterKind() {
        Object res =  this.wrapped().parameter_kind();
        if (res != null) {
            return (String) res;
        } else {
            return null;
        }
    }


    protected ParsedParameter wrapped() {
        return (ParsedParameter) this.rawModel;
    }
}
