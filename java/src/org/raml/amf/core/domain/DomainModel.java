package org.raml.amf.core.domain;

import api_modeling_framework.model.document.Node;
import api_modeling_framework.model.document.DocumentSourceMap;
import api_modeling_framework.model.domain.*;
import org.raml.amf.core.exceptions.InvalidModelException;
import org.raml.amf.core.Model;
import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 04/05/2017.
 */
public class DomainModel extends Model {

    static {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN);
    }

    public DomainModel(Object rawModel) {
        super(rawModel);
    }

    @Override
    public Object clojureModel() {
        return this.rawModel;
    }

    public static DomainModel fromRawModel(Object rawModel) {
        if (rawModel instanceof ParsedAPIDocumentation) {
            return new APIDocumentation((ParsedAPIDocumentation) rawModel);
        } else if (rawModel instanceof ParsedEndPoint) {
            return new EndPoint((ParsedEndPoint) rawModel);
        } else if (rawModel instanceof ParsedOperation) {
            return new Operation((ParsedOperation) rawModel);
        } else if (rawModel instanceof ParsedRequest) {
            return new Request((ParsedRequest) rawModel);
        } else if (rawModel instanceof ParsedResponse) {
            return new Response((ParsedResponse) rawModel);
        } else if (rawModel instanceof Payload) {
            return new Payload((ParsedPayload) rawModel);
        } else if (rawModel instanceof ParsedType) {
            return new Type((ParsedType) rawModel);
        } else {
            throw new InvalidModelException(new Exception("Unknown DomainModel class " + rawModel));
        }
    }

    public String getId() {
        return (String) wrappedNode().id();
    }

    public String getName() {
        return (String) wrappedNode().name();
    }

    public void setName(String name) {
        this.rawModel = Clojure.setKw(this.rawModel, "name", name);
    }

    public Boolean getAbstract() {
        return (Boolean) Clojure.getKw(this.rawModel, "abstract") || false;
    }

    public void setAbstract(Boolean abstractBool) {
        this.rawModel = Clojure.setKw(this.rawModel, "abstract", abstractBool);
    }

    public String getDescription() {
        return (String) wrappedNode().name();
    }

    public void setDescription(String description) {
        this.rawModel = Clojure.setKw(this.rawModel, "description", description);
    }

    public List<SourceMap> getSourceMaps() {
        List operations =  (List) this.wrappedNode().sources();
        List<DocumentSourceMap> tmp = Clojure.toJavaList(operations);
        ArrayList<SourceMap> eps = new ArrayList<>();
        for(DocumentSourceMap x : tmp) {
            SourceMap parsed = new SourceMap(x);
            eps.add(parsed);
        }

        return eps;
    }

    public void setSourceMaps(List<SourceMap> sourceMaps) {
        ArrayList<Object> raws = new ArrayList<>();
        for(SourceMap x : sourceMaps) {
            raws.add(x.clojureModel());
        }

        this.rawModel = Clojure.setKw(this.rawModel, "sources", Clojure.list(raws));
    }

    public List<DomainModel> getExtends() {
        List operations =  (List) Clojure.getKw(this.rawModel, "extends");
        List<Object> tmp = Clojure.toJavaList(operations);
        ArrayList<DomainModel> eps = new ArrayList<>();
        for(Object x : tmp) {
            eps.add(DomainModel.fromRawModel(x));
        }

        return eps;
    }

    public void setExtends(List<DomainModel> toExtend) {
        ArrayList<Object> raws = new ArrayList<>();
        for(DomainModel x : toExtend) {
            raws.add(x.clojureModel());
        }

        this.rawModel = Clojure.setKw(this.rawModel, "extends", Clojure.list(raws));
    }

    protected void setId(String id) {
        this.rawModel = Clojure.setKw(this.rawModel, "id", id);
    }
    protected Node wrappedNode() {
        return (Node) this.rawModel;
    }

    public String toString() {
        return (this.getId() + " :: " + super.toString());
    }
}
