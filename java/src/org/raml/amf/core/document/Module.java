package org.raml.amf.core.document;

import clojure.lang.IFn;
import org.raml.amf.core.domain.DomainModel;
import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * AMF Modules contains collections of DomainElements that can be re-used and referenced from other documents in the
 * Documentmodel.
 */
public class Module extends DocumentModel implements DeclaresDomainModel {
    public Module(Object rawModel) {
        super(rawModel);
    }

    /**
     * Declared DomainElements that can be re-used from other documents.
     * @return List of domain elements.
     */
    public List<DomainModel> declares() {
        IFn getFn = Clojure.var(Clojure.API_MODELING_FRAMEWORK_MODEL_DOCUMENT, "declares");
        List parsedElements = Clojure.toJavaList((List) getFn.invoke(this.clojureModel()));
        ArrayList<DomainModel> declared = new ArrayList<>();
        for(Object parsed : parsedElements) {
            declared.add(DomainModel.fromRawModel(parsed));
        }

        return declared;
    }
}
