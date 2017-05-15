package org.raml.amf.core.domain.shapes;

import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 15/05/2017.
 */

public class FileUploadShape extends Shape {

    public FileUploadShape(Object rawModel) {
        super(rawModel);
    }

    public List<String> getFileType() {
        List<Object> files = this.getValues(SHAPES_NS + "fileType");
        List<String> acc = new ArrayList<>(files.size());
        for(Object file : files) {
            acc.add(file.toString());
        }
        return acc;
    }

    public void setFileType(List<String> fileTypes) {
        this.setValues(SHAPES_NS + "fileType", fileTypes);
    }

    public static FileUploadShape build(String id) {
        Object acc = Clojure.emptyMap();
        acc = Clojure.set(acc, "@id", id);
        List<String> types = new ArrayList<>();
        types.add(id);
        acc = Clojure.set(acc, "@type", Clojure.list(types));
        return new FileUploadShape(acc);
    }
}
