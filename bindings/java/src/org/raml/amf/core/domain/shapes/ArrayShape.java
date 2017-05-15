package org.raml.amf.core.domain.shapes;

import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 15/05/2017.
 */

public class ArrayShape extends Shape {

    public ArrayShape(Object rawModel) {
        super(rawModel);
    }

    public List<Shape> getItems() {
        List<Object> shapes = Shape.ensureArray(Clojure.get(this.rawModel,SHAPES_NS + "items"));
        List<Shape> acc = new ArrayList<>(shapes.size());
        for(Object shape : shapes) {
            acc.add(Shape.fromRawModel(shape));
        }
        return acc;
    }

    public void setItems(List<Shape> items) {
        this.setRdfObject(SHAPES_NS + "items", items);
    }

    public static ArrayShape build(String id) {
        Object acc = Clojure.emptyMap();
        acc = Clojure.set(acc, "@id", id);
        List<String> types = new ArrayList<>();
        types.add(id);
        acc = Clojure.set(acc, "@type", Clojure.list(types));
        return new ArrayShape(acc);
    }
}
