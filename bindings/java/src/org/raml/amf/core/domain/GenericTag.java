package org.raml.amf.core.domain;

import api_modeling_framework.model.document.Tag;
import org.raml.amf.utils.Clojure;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * Tag included in a SourceMap
 * Tags are tuples with an identifier, describing the kind of information in the mapping and an arbitrary value.
 * Tags are also elements of the model, so they also have an associated URI.
 */
public class GenericTag extends DomainModel {

    static {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN);
    }

    public GenericTag(Object rawModel) {
        super(rawModel);
    }

    public GenericTag(String id, Object value) {
        super(Clojure.var(
                Clojure.API_MODELING_FRAMEWORK_MODEL_DOCUMENT,
                "api-modeling-framework.model.document/->APITagTag"
                ).invoke(id, value)
        );
    }

    public GenericTag(String id, String tagId, Object value) {
        super(Clojure.var(
                Clojure.API_MODELING_FRAMEWORK_MODEL_DOCUMENT,
                "api-modeling-framework.model.document/->APITagTag"
                ).invoke(id, value)
        );
        this.setTagId(tagId);
    }

    public String getTagId() {
        return (String) this.wrapped().tag_id();
    }

    public void setTagId(String tagId) {
        this.rawModel = Clojure.setKw(this.rawModel, "tag-id", tagId);
    }

    public Object getValue() {
        return this.wrapped().value();
    }

    public void setValue(Object value) {
        this.rawModel = Clojure.setKw(this.rawModel, "value", value);
    }

    protected Tag wrapped() {
        return (Tag) rawModel;
    }
}
