package org.raml.amf.core.domain.shapes;

import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 15/05/2017.
 */

public class ScalarShape extends Shape {

    public ScalarShape(Object rawModel) {
        super(rawModel);
    }

    public String getDatatype() {
        return this.getRdfId(SH_NS + "datatype");
    }

    public void setDatatype(String type) {
        this.setRdfId(SH_NS + "datatype", type);
    }

    public static ScalarShape build(String id) {
        Object acc = Clojure.emptyMap();
        acc = Clojure.set(acc, "@id", id);
        List<String> types = new ArrayList<>();
        types.add(id);
        acc = Clojure.set(acc, "@type", Clojure.list(types));
        return new ScalarShape(acc);
    }
}
