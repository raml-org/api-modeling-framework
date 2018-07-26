package org.raml.amf.core.document;

import org.raml.amf.core.domain.DomainModel;
import org.raml.amf.core.exceptions.InvalidModelException;

/**
 * Created by antoniogarrote on 04/05/2017.
 */
public interface EncodesDomainModel {

    /**
     * Encoded domain element described in the document element.
     * @return DomainElement encoded in the document.
     * @throws InvalidModelException
     */
    public DomainModel encodes() throws InvalidModelException;

}
