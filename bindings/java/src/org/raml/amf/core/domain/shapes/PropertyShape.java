package org.raml.amf.core.domain.shapes;

import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 15/05/2017.
 */

public class PropertyShape extends Shape {

    public PropertyShape(Object rawModel) {
        super(rawModel);
    }

    public String getPath() {
        return this.getRdfId(SH_NS + "path");
    }

    public void setPath(String path) {
        this.setRdfId(SH_NS + "path", path);
    }

    public String getPropertyLabel() {
        return this.getValue(SHAPES_NS + "propertyLabel");
    }

    public void setPropertyLabel(String label) {
        this.setValue(SHAPES_NS + "propertyLabel", label);
    }

    public Integer getMinCount() {
        return this.getValue(SH_NS + "minCount");
    }

    public void setMinCount(Integer count) {
        this.setValue(SH_NS + "minCount", count);
    }

    public Integer getMaxCount() {
        return this.getValue(SH_NS + "maxCount");
    }

    public void setMaxCount(Integer count) {
        this.setValue(SH_NS + "maxCount", count);
    }

    public String getDatatype() {
        return this.getRdfId(SH_NS + "datatype");
    }

    public void setDatatype(String type) {
        this.setRdfId(SH_NS + "datatype", type);
    }

    public Shape getNode() {
        Object object = this.getRdfObject(SH_NS + "node");
        if (object != null)
            return Shape.fromRawModel(object);
        return null;
    }

    public void setNode(Shape object) {
        if (object != null) {
            this.setRdfObject(SH_NS + "node", object.clojureModel());
        } else {
            this.setRdfObject(SH_NS + "node", null);
        }
    }

    public boolean getOrdered() {
        Boolean res = this.getValue(SHAPES_NS + "ordered");
        if (res == null) {
            return false;
        } else {
            return res;
        }
    }

    public void setOrdered(Boolean ordered) {
        this.setValue(SH_NS + "ordered", ordered);
    }

    public static PropertyShape build(String id) {
        Object acc = Clojure.emptyMap();
        acc = Clojure.set(acc, "@id", id);
        List<String> types = new ArrayList<>();
        types.add(id);
        acc = Clojure.set(acc, "@type", Clojure.list(types));
        return new PropertyShape(acc);
    }
}
