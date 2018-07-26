package org.raml.amf.core.document;

import clojure.lang.IFn;
import org.raml.amf.core.domain.DomainModel;
import org.raml.amf.core.exceptions.InvalidModelException;
import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * AMF Documents encode the main element of a description in a particular Domain Model
 * For example, in RAML/HTTP, the main domain element is an APIDescription.
 *
 * Since AMF Documents encode Domain elements they behave like Fragments
 * AMF Documents can also contains  declarations of domain elements to be used in the description of the domain.
 * From this point of view Documents also behave like Modules.
 */
public class Document extends DocumentModel implements EncodesDomainModel, DeclaresDomainModel {
    public Document(Object rawModel) {
        super(rawModel);
    }

    /**
     * Encoded domain element. It's considered to be the root element of a stand-alone description, not a domain element
     * to be re-used and reference
     * @return DomainElement encoded in the document.
     * @throws InvalidModelException
     */
    public DomainModel encodes() throws InvalidModelException {
        IFn getFn = Clojure.var(Clojure.API_MODELING_FRAMEWORK_MODEL_DOCUMENT, "encodes");
        return DomainModel.fromRawModel(getFn.invoke(this.clojureModel()));
    }

    /**
     * List of domain elements declared in the document to be referenced in the encoded element.
     * They are supposed to be private to the description and not meant to be re-used as in Modules.
     * @return
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
