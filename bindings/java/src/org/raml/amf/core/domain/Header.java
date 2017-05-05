package org.raml.amf.core.domain;

import api_modeling_framework.model.domain.ParsedParameter;
import org.raml.amf.utils.Clojure;

/**
 * Created by antoniogarrote on 04/05/2017.
 */


public class Header extends GenericParameter {

    static {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN);
    }

    public Header(ParsedParameter rawModel) {
        super(rawModel);
    }

    public Header(String id) {
        super((ParsedParameter) Clojure.var(
                Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN,
                "api-modeling-framework.model.domain/map->ParsedParameter"
                ).invoke(Clojure.map())
        );
        this.setId(id);
        this.setParameterKindInternal("header");
    }
}
