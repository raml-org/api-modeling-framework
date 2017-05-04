package org.raml.amf.core.document;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import org.raml.amf.core.domain.DomainModel;
import org.raml.amf.core.exceptions.InvalidModelException;
import org.raml.amf.utils.Clojure;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * AMF Fragments encode a single DomainElement that can be referenced and re-used in other documents.
 */
public class Fragment extends DocumentModel implements EncodesDomainModel {
    static {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_CORE);
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_MODEL_DOCUMENT);
    }

    public Fragment(Object rawModel) {
        super(rawModel);
    }

    /**
     * Encoded Domain element that can referenced from other documents in the DocumentModel
     * @return
     * @throws InvalidModelException
     */
    public DomainModel encodes() throws InvalidModelException {
        IFn getFn = Clojure.var(Clojure.API_MODELING_FRAMEWORK_MODEL_DOCUMENT, "encodes");
        return DomainModel.fromRawModel(getFn.invoke(this.clojureModel()));
    }
}
