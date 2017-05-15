package org.raml.amf.core.domain;

import api_modeling_framework.model.domain.ParsedType;
import clojure.lang.IFn;
import org.raml.amf.core.domain.shapes.Shape;
import org.raml.amf.utils.Clojure;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * Data Shape that describing a set of constraints over an operation unit payload
 */
public class Type extends DomainModel {
    static {
        Clojure.require(Clojure.CHESHIRE_CORE);
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN);
    }

    public Type(Object rawModel) {
        super(rawModel);
    }

    public Type(String id) {
        super(Clojure.var(
                Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN,
                "api-modeling-framework.model.domain/map->ParsedType"
                ).invoke(Clojure.map())
        );
        this.setId(id);
    }

    /**
     * JSON-LD string containing a SHACL shape that can be used to validate payloads for this operation unit
     * @return
     */
    public String getShapeJSONLD() {
        IFn generateStringFn = Clojure.var(Clojure.CHESHIRE_CORE, "generate-string");
        return (String) generateStringFn.invoke(this.wrapped().shape());
    }

    /**
     * Sets the SHACL shape for the payloads of this operation unit
     * @param shaclShape valid SHACL shape encoded as JSON-LD string
     */
    public void setShapeJSONLD(String shaclShape) {
        IFn parseStringFn = Clojure.var(Clojure.CHESHIRE_CORE, "parse-string");
        this.rawModel = Clojure.setKw(this.rawModel, "shape", parseStringFn.invoke(shaclShape));
    }

    /**
     * SHACL shape that can be used to validate payloads for this operation unit
     * @return
     */
    public Shape getShape() {
        return Shape.fromRawModel(this.wrapped().shape());
    }

    /**
     * Sets the SHACL shape for the payloads of this operation unit
     * @param shape valid SHACL shape
     */
    public void setShape(Shape shape){
        this.rawModel = Clojure.setKw(this.rawModel, "shape", shape.clojureModel());
    }

    public ParsedType wrapped() {
        return (ParsedType) this.rawModel;
    }
}
