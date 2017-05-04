package org.raml.amf.core.domain;

import api_modeling_framework.model.domain.ParsedPayload;
import api_modeling_framework.model.domain.ParsedType;
import org.raml.amf.core.exceptions.InvalidModelException;
import org.raml.amf.utils.Clojure;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * Schema information for a Payload associated to a particular media-type
 */
public class Payload extends DomainModel {

    static {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN);
    }

    public Payload(ParsedPayload rawModel) {
        super(rawModel);
    }

    public Payload(String id) {
        super(Clojure.var(
                Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN,
                "api-modeling-framework.model.domain/map->ParsedPayload"
                ).invoke(Clojure.map())
        );
        this.setId(id);
    }

    public String getMediaType() {
        return (String) this.wrapped().media_type();
    }

    public void setMediaType(String mediaType) {
        this.rawModel = Clojure.setKw(this.rawModel, "media-type", mediaType);
    }

    /**
     * Schema information for the payload
     * @return
     */
    public Type getSchema() {
        ParsedType type =  (ParsedType) this.wrapped().schema();
        return new Type(type);
    }

    public void setSchema(Type type) {
        this.rawModel = Clojure.setKw(this.wrapped(), "schema", type.clojureModel());
    }

    private ParsedPayload wrapped() {
        return (ParsedPayload) this.rawModel;
    }
}
