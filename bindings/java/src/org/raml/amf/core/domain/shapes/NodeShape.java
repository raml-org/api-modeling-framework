package org.raml.amf.core.domain.shapes;

import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 15/05/2017.
 */

public class NodeShape extends Shape {
    public NodeShape(Object rawModel) {
        super(rawModel);
    }

    public List<PropertyShape> getPropertyShapes() {
        List<Object> shapes = Shape.ensureArray(Clojure.get(this.rawModel,SH_NS + "property"));
        List<PropertyShape> acc = new ArrayList<>(shapes.size());
        for(Object shape : shapes) {
            acc.add(new PropertyShape(shape));
        }
        return acc;
    }

    public boolean getClosed() {
        Boolean closed = this.getValue(SH_NS + "closed");
        if (closed == null) {
            return false;
        } else {
            return closed;
        }
    }

    public void setClosed(Boolean closed) {
        this.setValue(SH_NS + "closed", closed);
    }

    public static NodeShape build(String id) {
        Object acc = Clojure.emptyMap();
        acc = Clojure.set(acc, "@id", id);
        List<String> types = new ArrayList<>();
        types.add(id);
        acc = Clojure.set(acc, "@type", Clojure.list(types));
        return new NodeShape(acc);
    }
}
