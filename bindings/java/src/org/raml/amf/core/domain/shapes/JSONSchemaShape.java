package org.raml.amf.core.domain.shapes;

import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 15/05/2017.
 */

public class JSONSchemaShape extends Shape {

    public JSONSchemaShape(Object rawModel) {
        super(rawModel);
    }

    public String getSchemaRaw() {
        return this.getValue(SHAPES_NS + "schemaRaw");
    }

    public void setSchemaRaw(String schema) {
        this.setValue(SHAPES_NS + "schemaRaw", schema);
    }

    public static JSONSchemaShape build(String id) {
        Object acc = Clojure.emptyMap();
        acc = Clojure.set(acc, "@id", id);
        List<String> types = new ArrayList<>();
        types.add(id);
        acc = Clojure.set(acc, "@type", Clojure.list(types));
        return new JSONSchemaShape(acc);
    }
}
