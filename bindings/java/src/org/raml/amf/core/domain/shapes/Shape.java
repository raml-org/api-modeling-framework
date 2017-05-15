package org.raml.amf.core.domain.shapes;

import org.raml.amf.core.domain.DomainModel;
import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 15/05/2017.
 */
public class Shape extends DomainModel {

    public final static String SH_NS = "http://www.w3.org/ns/shacl#";
    public final static String SHAPES_NS = "http://raml.org/vocabularies/shapes#";


    public Shape(Object rawModel) {
        super(rawModel);
    }

    public String getId() {
        return (String) Clojure.get(this.rawModel,"@id");
    }

    public List<Shape> getInherits() {
        List<Object> inherits = this.getRdfObjects(SHAPES_NS + "inherits");
        List<Shape> acc = new ArrayList<>(inherits.size());
        for (Object e : inherits) {
            acc.add(Shape.fromRawModel(e));
        }
        return acc;
    }

    public void setInherits(List<Shape> shapes) {
        this.setRdfObject(SHAPES_NS + "inherits", shapes);
    }

    public static Shape fromRawModel(Object rawModel) {

        if (Shape.findShapeClass(rawModel, SH_NS + "NodeShape")) {
            return new NodeShape(rawModel);
        } else if (Shape.findShapeClass(rawModel, SH_NS + "PropertyShape")) {
            return new PropertyShape(rawModel);
        } else if (Shape.findShapeClass(rawModel, SHAPES_NS + "Scalar")){
            return new ScalarShape(rawModel);
        } else if (Shape.findShapeClass(rawModel, SHAPES_NS + "Array")) {
            return new ArrayShape(rawModel);
        } else if (Shape.findShapeClass(rawModel, SHAPES_NS + "FileUpload")) {
            return new  FileUploadShape(rawModel);
        } else if (Shape.findShapeClass(rawModel, SHAPES_NS + "XMLSchema")) {
            return new XMLSchemaShape(rawModel);
        } else if (Shape.findShapeClass(rawModel, SHAPES_NS + "JSONSchema")) {
            return new JSONSchemaShape(rawModel);
        } else {
            return new Shape(rawModel);
        }
    }

    protected static boolean findShapeClass(Object shape, String shapeClass) {
        List<Object> types = Shape.ensureArray(Clojure.get(shape, "@type"));
        for(Object type : types) {
            if (shapeClass.equals(type)) {
                return true;
            }
        }
        return false;
    }

    public static Shape build(String id) {
        Object acc = Clojure.emptyMap();
        acc = Clojure.set(acc, "@id", id);
        List<String> types = new ArrayList<>();
        types.add(id);
        acc = Clojure.set(acc, "@type", Clojure.list(types));
        return new Shape(acc);
    }

    protected <T> T getValue(String p) {
        List<Object> res = Shape.ensureArray(Clojure.get(this.rawModel,p));
        if (res.size() > 0) {
            Object e = res.get(0);
            e = Clojure.get(e, "@value");
            try {
                return (T) e;
            } catch (Exception ex) {
                return null;
            }
        } else {
            return null;
        }
    }

    protected <T> List<T> getValues(String p)  {
        List<Object> res = Shape.ensureArray(Clojure.get(this.rawModel, p));
        List<T> acc = new ArrayList<>(res.size());
        if (res.size() > 0) {
            for(Object x : res) {
                try {
                    acc.add((T) x);
                } catch(Exception e) {
                    // ignore
                }
            }

        }
        return acc;
    }

    protected void setValue(String p, Object x) {
        if (x == null) {
            Clojure.remove(this.rawModel, p);
        } else {
            List<Object> acc = new ArrayList<>();
            acc.add(Clojure.set(Clojure.map(), "@value", x));
            this.rawModel = Clojure.set(this.rawModel, p, Clojure.list(acc));
        }
    }

    protected void setValues(String p, Object x) {
        if (x == null) {
            this.rawModel = Clojure.remove(this.rawModel, p);
        } else {
            List<Object> xs = Shape.ensureArray(x);
            List<Object> xsp = new ArrayList(xs.size());
            for(Object e : xs) {
                xsp.add(Clojure.set(Clojure.emptyMap(), "@value", e));
            }
            this.rawModel = Clojure.set(this.rawModel, p, Clojure.list(xsp));
        }
    }

    protected void setRdfObject(String p, Object object) {
        if (object == null) {
            this.rawModel = Clojure.remove(this.rawModel, p);
        } else {
            this.rawModel = Clojure.set(this.rawModel, p, Shape.ensureArray(object));
        }
    }

    protected Object getRdfObject(String p) {
        List<Object> res = Shape.ensureArray(Clojure.get(this.rawModel,p));
        if (res.size() > 0) {
            return res.get(0);
        } else {
            return null;
        }
    }

    protected List<Object> getRdfObjects(String p) {
        return Shape.ensureArray(Clojure.get(this.rawModel, p));
    }

    protected String getRdfId(String p) {
        List<Object> res = Shape.ensureArray(Clojure.get(this.rawModel,p));
        if (res.size() > 0) {
            return (String) Clojure.get(res.get(0), "@id");
        } else {
            return null;
        }
    }

    protected void setRdfId(String p, String id) {
        if (id == null) {
            this.rawModel = Clojure.remove(this.rawModel,p);
        } else {
            Object idMap = Clojure.set(Clojure.emptyMap(), "@id", id);
            List acc = new ArrayList();
            acc.add(idMap);
            this.rawModel = Clojure.set(this.rawModel, p, Clojure.list(acc));
        }
    }

    protected static List<Object> ensureArray(Object x) {
        if (x == null) {
            return new ArrayList<>();
        } else if(x instanceof List) {
            return Clojure.toJavaList((List) x);
        } else {
            List<Object> acc = new ArrayList();
            acc.add(x);
            return acc;
        }
    }

}
