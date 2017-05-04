package org.raml.amf.core.domain;

import api_modeling_framework.model.document.DocumentSourceMap;
import api_modeling_framework.model.document.Tag;
import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 04/05/2017.
 */


public class SourceMap extends DomainModel {

    public SourceMap(DocumentSourceMap rawModel) {
        super(rawModel);
    }

    protected DocumentSourceMap wrapped() {
        return (DocumentSourceMap) this.rawModel;
    }

    public String getSource() {
        return (String) this.wrapped().source();
    }

    public void setSource(String source) {
        this.rawModel = Clojure.setKw(this.rawModel, "source", source);
    }

    public List<GenericTag> getTags() {
        List operations =  (List) this.wrapped().tags();
        List<Tag> tmp = Clojure.toJavaList(operations);
        ArrayList<GenericTag> eps = new ArrayList<>();
        for(Tag x : tmp) {
            GenericTag parsed = new GenericTag(x);
            eps.add(parsed);
        }

        return eps;
    }

    public void setTags(List<GenericTag> tags) {
        ArrayList<Object> raws = new ArrayList<>();
        for(GenericTag x : tags) {
            raws.add(x.clojureModel());
        }

        this.rawModel = Clojure.setKw(this.rawModel, "tags", Clojure.list(raws));
    }

}
